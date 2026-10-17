package vn.deadline.tracker.security;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.*;
import java.io.Serial;
public final class TrackerPrincipal implements UserDetails,CredentialsContainer {
    @Serial private static final long serialVersionUID=1L;
    private final String id,username;private String password;private final long version;
    public TrackerPrincipal(String id,String username,String password,long version){this.id=id;this.username=username;this.password=password;this.version=version;}
    public String id(){return id;}public long version(){return version;}
    public String getUsername(){return username;}public String getPassword(){return password;}
    public Collection<? extends GrantedAuthority> getAuthorities(){return List.of(new SimpleGrantedAuthority("USER"));}
    public void eraseCredentials(){password=null;}
}
