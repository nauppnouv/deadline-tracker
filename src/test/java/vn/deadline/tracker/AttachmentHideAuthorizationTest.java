package vn.deadline.tracker;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.util.HtmlUtils;
import vn.deadline.tracker.service.AccountService;
import vn.deadline.tracker.service.AttachmentService;
import vn.deadline.tracker.service.InvitationService;
import vn.deadline.tracker.service.WorkService;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.address=127.0.0.1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttachmentHideAuthorizationTest extends DatabaseTest {
    private static final String PASSWORD = "Synthetic R1 regression passphrase 123!";
    private static final byte[] CONTENT = "Synthetic R1 attachment\n".getBytes(StandardCharsets.UTF_8);
    @Value("${local.server.port}") int port;
    @Value("${app.attachments.root}") String attachmentRoot;
    @Autowired AccountService accounts;
    @Autowired WorkService work;
    @Autowired InvitationService invitations;
    @Autowired AttachmentService files;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    record User(String id, String username) {}
    User actor, owner, uploader;
    String ownedGroup, resourceGroup, task, fileId;
    Map<String, Object> originalFile;
    List<Map<String, Object>> originalPins;
    Path blob;

    @BeforeAll void verifyDisposableMariaDb() throws Exception {
        try (var connection = dataSource.getConnection()) {
            String connectedUrl = connection.getMetaData().getURL().split("\\?", 2)[0];
            assertEquals(DB.getJdbcUrl(), connectedUrl);
        }
        String database = jdbc.queryForObject("SELECT DATABASE()", String.class);
        String principal = jdbc.queryForObject("SELECT CURRENT_USER()", String.class);
        String version = jdbc.queryForObject("SELECT VERSION()", String.class);
        assertEquals("deadline_test", database);
        assertTrue(principal.startsWith("deadline_test@"));
        assertTrue(version.startsWith("11.8.9"));
        assertFalse(DB.isShouldBeReused());
        System.out.printf("R1 isolation: database=%s principal=%s version=%s container=%s jdbc=%s%n",
                database, principal, version, DB.getContainerId(), DB.getJdbcUrl());
    }

    @BeforeEach void seed() throws Exception {
        actor = user();
        owner = user();
        uploader = user();
        ownedGroup = work.createGroup(actor.id(), "Actor owns this group", "").getId();
        resourceGroup = work.createGroup(owner.id(), "Resource group", "").getId();
        invitations.join(actor.id(), invitations.issue(owner.id(), resourceGroup));
        invitations.join(uploader.id(), invitations.issue(owner.id(), resourceGroup));
        task = work.createTask(owner.id(), resourceGroup, "R1 resource task", "",
                owner.id(), Instant.now().plusSeconds(3600)).getId();
        var uploaded = files.upload(uploader.id(), resourceGroup, task,
                new MockMultipartFile("file", "regression.txt", "text/plain", CONTENT));
        fileId = uploaded.id;
        files.pin(owner.id(), resourceGroup, task, fileId, true);
        originalFile = fileRow();
        originalPins = pinRows();
        blob = Path.of(attachmentRoot).resolve(uploaded.storageKey + ".blob");
        assertArrayEquals(CONTENT, Files.readAllBytes(blob));
        assertEquals(1, originalPins.size());
        assertEquals("MEMBER", jdbc.queryForObject(
                "SELECT role FROM group_member WHERE group_id=? AND user_id=?",
                String.class, resourceGroup, actor.id()));
    }

    @Test void memberMustNotBorrowOwnershipFromAnotherGroupToHideFile() throws Exception {
        try (var client = login(actor)) {
            var download = client.send(request(filePath(resourceGroup, task, fileId)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, download.statusCode());
            assertArrayEquals(CONTENT, download.body());
            int status = hide(client, filePath(ownedGroup, task, fileId), true);
            assertAll(() -> assertEquals(404, status, "An unrelated group's Owner cannot hide this file"),
                    this::assertUnchanged);
        }
    }

    @Test void memberCannotHideAnotherUploadInTheCorrectGroup() throws Exception {
        assertRejected(actor, filePath(resourceGroup, task, fileId), true, 403);
    }

    @Test void mismatchedTaskAndMissingFileReturn404WithoutMutation() throws Exception {
        String otherTask = work.createTask(owner.id(), resourceGroup, "Another task", "",
                owner.id(), Instant.now().plusSeconds(3600)).getId();
        assertRejected(owner, filePath(resourceGroup, otherTask, fileId), true, 404);
        assertRejected(owner, filePath(resourceGroup, task, UUID.randomUUID().toString()), true, 404);
    }

    @Test void outsiderCannotUseTheUploaderExceptionInAnotherGroup() throws Exception {
        assertRejected(uploader, filePath(ownedGroup, task, fileId), true, 404);
    }

    @Test void missingCsrfCannotHideEvenTheUploadersOwnFile() throws Exception {
        assertRejected(uploader, filePath(resourceGroup, task, fileId), false, 403);
    }

    @Test void resourceGroupOwnerCanHideAndRemovePinWhileRetainingBlob() throws Exception {
        assertAllowed(owner);
    }

    @Test void uploaderCanHideAndRemovePinWhileRetainingBlob() throws Exception {
        assertAllowed(uploader);
    }

    @Test void inconsistentAttachmentGroupFailsClosedBeforePermissionOrMutation() throws Exception {
        // V7's separate foreign keys allow this synthetic inconsistent row.
        invitations.join(uploader.id(), invitations.issue(actor.id(), ownedGroup));
        jdbc.update("UPDATE task_attachment SET group_id=? WHERE id=?", ownedGroup, fileId);
        originalFile = fileRow();
        assertRejected(owner, filePath(resourceGroup, task, fileId), true, 404);
    }

    private void assertRejected(User user, String path, boolean csrf, int expected) throws Exception {
        try (var client = login(user)) {
            int status = hide(client, path, csrf);
            assertAll(() -> assertEquals(expected, status), this::assertUnchanged);
        }
    }

    private void assertAllowed(User user) throws Exception {
        try (var client = login(user)) {
            var path = filePath(resourceGroup, task, fileId);
            assertEquals(302, hide(client, path, true));
            var row = fileRow();
            assertNotNull(row.get("hidden_at"));
            assertEquals(user.id(), row.get("hidden_by"));
            assertEquals("READY", row.get("lifecycle"));
            assertEquals(originalFile.get("storage_key"), row.get("storage_key"));
            assertEquals(originalFile.get("sha256"), row.get("sha256"));
            assertEquals(originalFile.get("size_bytes"), row.get("size_bytes"));
            assertTrue(pinRows().isEmpty());
            assertArrayEquals(CONTENT, Files.readAllBytes(blob));
            assertEquals(404, client.send(request(path).GET().build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode());
        }
    }

    private void assertUnchanged() throws Exception {
        assertAll(() -> assertEquals(originalFile, fileRow(), "Attachment DB row must remain unchanged"),
                () -> assertEquals(originalPins, pinRows(), "Pin DB row must remain unchanged"),
                () -> assertArrayEquals(CONTENT, Files.readAllBytes(blob)));
    }

    private Map<String, Object> fileRow() {
        return jdbc.queryForMap("SELECT * FROM task_attachment WHERE id=?", fileId);
    }

    private List<Map<String, Object>> pinRows() {
        return jdbc.queryForList("SELECT * FROM task_pin WHERE task_id=? AND attachment_id=? ORDER BY id",
                task, fileId);
    }

    private User user() {
        String username = "r1" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        return new User(accounts.register(username, "Synthetic R1 user", PASSWORD), username);
    }

    private HttpClient login(User user) throws Exception {
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        try {
            String body = "username=" + encode(user.username()) + "&password=" + encode(PASSWORD)
                    + "&_csrf=" + encode(token(client, "/login"));
            assertEquals(302, client.send(request("/login").header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode());
            return client;
        } catch (Exception | AssertionError error) {
            client.close();
            throw error;
        }
    }

    private int hide(HttpClient client, String path, boolean csrf) throws Exception {
        String body = csrf ? "_csrf=" + encode(token(client, "/account")) : "";
        return client.send(request(path + "/hide").header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private String token(HttpClient client, String path) throws Exception {
        var response = client.send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        var matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(response.body());
        assertTrue(matcher.find(), "A real rendered CSRF input is required");
        return HtmlUtils.htmlUnescape(matcher.group(1));
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(20));
    }

    private String filePath(String group, String taskId, String attachment) {
        return "/groups/" + group + "/tasks/" + taskId + "/attachments/" + attachment;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
