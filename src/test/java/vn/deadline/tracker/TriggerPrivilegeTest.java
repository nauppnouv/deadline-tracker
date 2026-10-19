package vn.deadline.tracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TriggerPrivilegeTest extends DatabaseTest {
    @Test @DisplayName("F-06 scratch principal with TRIGGER can create and drop triggers, not tables")
    void triggerPrivilegeIsTriggerDdlOnly() throws Exception {
        String suffix=UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String database="trigger_probe_"+suffix;
        String user="trigger_probe_"+suffix;
        String password="Probe"+suffix+"!";
        try {
            var setup=DB.execInContainer("bash", "-lc", """
                MYSQL_PWD="${MARIADB_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}" mariadb -h127.0.0.1 -uroot <<'SQL'
                CREATE DATABASE `DB_NAME`;
                CREATE TABLE `DB_NAME`.probe_table (id INT);
                CREATE USER 'DB_USER'@'127.0.0.1' IDENTIFIED BY 'DB_PASSWORD';
                GRANT TRIGGER ON `DB_NAME`.* TO 'DB_USER'@'127.0.0.1';
                SQL
                """.replace("DB_NAME", database).replace("DB_USER", user).replace("DB_PASSWORD", password));
            assertEquals(0, setup.getExitCode(), setup.getStderr());

            var create=DB.execInContainer("bash", "-lc", "MYSQL_PWD='"+password+"' mariadb -h127.0.0.1 -u'"+user+"' -e \"CREATE TRIGGER "+database+".probe_trigger BEFORE INSERT ON "+database+".probe_table FOR EACH ROW SET @trigger_probe=1\"");
            assertEquals(0, create.getExitCode(), create.getStderr());
            var drop=DB.execInContainer("bash", "-lc", "MYSQL_PWD='"+password+"' mariadb -h127.0.0.1 -u'"+user+"' -e \"DROP TRIGGER "+database+".probe_trigger\"");
            assertEquals(0, drop.getExitCode(), drop.getStderr());
            var createTable=DB.execInContainer("bash", "-lc", "MYSQL_PWD='"+password+"' mariadb -h127.0.0.1 -u'"+user+"' -e \"CREATE TABLE "+database+".not_allowed (id INT)\"");
            assertNotEquals(0, createTable.getExitCode(), "TRIGGER alone must not grant CREATE TABLE");
        } finally {
            DB.execInContainer("bash", "-lc", """
                MYSQL_PWD="${MARIADB_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}" mariadb -h127.0.0.1 -uroot <<'SQL'
                DROP DATABASE IF EXISTS `DB_NAME`;
                DROP USER IF EXISTS 'DB_USER'@'127.0.0.1';
                SQL
                """.replace("DB_NAME", database).replace("DB_USER", user));
        }
    }
}
