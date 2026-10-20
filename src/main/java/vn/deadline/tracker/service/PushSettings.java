package vn.deadline.tracker.service;

import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PushSettings {
    public final boolean enabled;
    public final String publicKey,subject;
    private final String privateKey;
    public PushSettings(@Value("${app.push.enabled:false}") boolean enabled,
                        @Value("${app.push.public-key:}") String publicKey,
                        @Value("${app.push.private-key-file:}") String keyFile,
                        @Value("${app.push.subject:}") String subject)throws Exception {
        this.enabled=enabled;this.publicKey=publicKey;this.subject=subject;
        if(!enabled){privateKey="";return;}
        try {
            Path file=Path.of(keyFile).toAbsolutePath().normalize();
            if(Files.isSymbolicLink(file)||!Files.isRegularFile(file)||Files.size(file)>256
                ||file.toRealPath().startsWith(Path.of(System.getProperty("user.dir")).toRealPath())
                ||!publicKey.matches("[A-Za-z0-9_-]{87}=?")
                ||!subject.matches("mailto:[^\\s]+@[^\\s]+|https://[^\\s]+"))throw new IllegalArgumentException();
            privateKey=Files.readString(file).strip();
            if(!privateKey.matches("[A-Za-z0-9_-]{43}=?"))throw new IllegalArgumentException();
            java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            var publicPoint=(org.bouncycastle.jce.interfaces.ECPublicKey)nl.martijndwars.webpush.Utils.loadPublicKey(publicKey);
            var privateScalar=(org.bouncycastle.jce.interfaces.ECPrivateKey)nl.martijndwars.webpush.Utils.loadPrivateKey(privateKey);
            if(!privateScalar.getParameters().getG().multiply(privateScalar.getD()).normalize().equals(publicPoint.getQ().normalize()))throw new IllegalArgumentException();
        } catch(Exception error){throw new IllegalStateException("Invalid Web Push configuration (key file must be private and outside workspace)");}
    }
    String privateKey(){return privateKey;}
}
