package vn.deadline.tracker;

import java.util.*;
import java.util.regex.Pattern;
import org.springframework.web.util.HtmlUtils;
import static org.junit.jupiter.api.Assertions.*;

/** Read the actual rendered form, never substitute a service version or default. */
final class HtmlForms {
    private HtmlForms() {}
    private static Map<String,String> attributes(String tag) {
        var result=new HashMap<String,String>();
        var matcher=Pattern.compile("([\\w:-]+)\\s*=\\s*\"([^\"]*)\"").matcher(tag);
        while(matcher.find())result.put(matcher.group(1),HtmlUtils.htmlUnescape(matcher.group(2)));
        return result;
    }
    static String version(String html,String action) {
        var forms=Pattern.compile("<form\\b([^>]*)>(.*?)</form>",Pattern.DOTALL).matcher(html);
        while(forms.find()) {
            if(!action.equals(attributes(forms.group(1)).get("action")))continue;
            var inputs=Pattern.compile("<input\\b[^>]*>").matcher(forms.group(2));
            var versions=new ArrayList<String>();
            while(inputs.find()) {
                var attr=attributes(inputs.group());
                if(!"version".equals(attr.get("name")))continue;
                assertEquals("hidden",attr.get("type"),"Version must be a hidden input in "+action);
                String value=attr.get("value");
                assertNotNull(value,"Missing version value in "+action);
                assertTrue(value.matches("[0-9]+"),"Invalid rendered version in "+action);
                versions.add(value);
            }
            assertEquals(1,versions.size(),"Exactly one hidden version required in "+action);
            return versions.getFirst();
        }
        return fail("Rendered form not found: "+action);
    }
}
