package vn.deadline.tracker.security;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import vn.deadline.tracker.persistence.AccountRepository;
import java.util.Locale;
@Configuration @ConditionalOnWebApplication(type=ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {
    @Bean org.springframework.session.web.http.CookieSerializer sessionCookies(org.springframework.core.env.Environment environment){
        var serializer=new org.springframework.session.web.http.DefaultCookieSerializer();
        serializer.setCookieName("SESSION");serializer.setUseHttpOnlyCookie(true);serializer.setSameSite("Lax");
        serializer.setUseSecureCookie(environment.acceptsProfiles(org.springframework.core.env.Profiles.of("prod")));return serializer;
    }
    @Bean UserDetailsService userDetails(AccountRepository accounts){return raw->{
        String name=raw==null?"":raw.strip().toLowerCase(Locale.ROOT);
        var row=accounts.findByUsername(name).filter(a->a.enabled).orElseThrow(()->new UsernameNotFoundException("Invalid credentials"));
        return new TrackerPrincipal(row.id,row.username,row.passwordHash,row.version);
    };}
    @Bean SecurityFilterChain security(HttpSecurity http,UserDetailsService users,PasswordEncoder encoder,RateLimiter limiter,AccountRepository accounts)throws Exception{
        var provider=new DaoAuthenticationProvider(users);provider.setPasswordEncoder(encoder);http.authenticationProvider(provider);
        http.authorizeHttpRequests(a->a.requestMatchers("/login","/register","/error","/css/**","/js/**").permitAll().anyRequest().authenticated());
        http.formLogin(f->f.loginPage("/login").failureUrl("/login?error").defaultSuccessUrl("/groups",true).permitAll());
        http.requestCache(c->c.requestCache(new org.springframework.security.web.savedrequest.NullRequestCache()));
        http.logout(l->l.logoutUrl("/logout").logoutSuccessUrl("/login?logout").invalidateHttpSession(true).clearAuthentication(true).deleteCookies("SESSION"));
        http.exceptionHandling(e->e.authenticationEntryPoint((request,response,error)->{if(request.getRequestURI().substring(request.getContextPath().length()).startsWith("/groups/"))response.sendError(404);else response.sendRedirect(request.getContextPath()+"/login");}));
        http.headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'"))
            .referrerPolicy(r->r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)).frameOptions(f->f.deny()));
        http.addFilterBefore(new AuthAttemptFilter(limiter),UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new AccountStateFilter(accounts),AuthorizationFilter.class);
        return http.build();
    }
}
