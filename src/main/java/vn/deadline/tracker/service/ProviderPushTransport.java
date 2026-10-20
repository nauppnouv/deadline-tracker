package vn.deadline.tracker.service;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import nl.martijndwars.webpush.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

@Component
public class ProviderPushTransport implements PushTransport {
    private final PushSettings settings;private final PushEndpointPolicy policy;
    public ProviderPushTransport(PushSettings settings,PushEndpointPolicy policy){this.settings=settings;this.policy=policy;}
    /** Exposes only the upstream encryption/VAPID API; never calls its unrestricted HTTP send methods. */
    private static final class Encoder extends AbstractPushService<Encoder> {
        Encoder(PushSettings s)throws Exception {super(s.publicKey,s.privateKey(),s.subject);}
        HttpRequest encode(Notification n)throws Exception{return prepareRequest(n,Encoding.AES128GCM);}
    }
    @Override public Result send(Message m)throws Exception {
        if(!settings.enabled)throw new IllegalStateException("Web Push is disabled");
        var uri=policy.endpoint(m.endpoint());
        Security.addProvider(new BouncyCastleProvider());
        byte[] payload=("{\"id\":\""+java.util.UUID.fromString(m.notificationId())+"\"}").getBytes(StandardCharsets.UTF_8);
        var request=new Encoder(settings).encode(new Notification(uri.toASCIIString(),m.publicKey(),m.auth(),payload,300));
        // Apache connects directly to addresses returned by this checked resolver. TLS still
        // verifies the original provider hostname. No proxy, redirects, retries or reused socket.
        try(var client=HttpClients.custom().setDnsResolver(policy::resolve)
            .setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE).disableRedirectHandling()
            .disableAutomaticRetries().disableCookieManagement().setDefaultRequestConfig(RequestConfig.custom()
                .setConnectTimeout(3000).setConnectionRequestTimeout(3000).setSocketTimeout(5000).build()).build()) {
            var post=new HttpPost(uri);request.getHeaders().forEach(post::setHeader);
            post.setEntity(new ByteArrayEntity(request.getBody()));
            try(var response=client.execute(post)) {
                var retry=response.getFirstHeader("Retry-After");
                return new Result(response.getStatusLine().getStatusCode(),retry==null?null:retry.getValue());
            }
        }
    }
}
