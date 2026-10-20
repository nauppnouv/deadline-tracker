package vn.deadline.tracker.service;

import java.net.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Provider registry and the DNS resolver used by the actual socket connection. */
@Component
public class PushEndpointPolicy {
    public URI endpoint(String value) {
        try {
            if(value==null||value.length()>2048||!value.matches("[\\x21-\\x7e]+"))throw new IllegalArgumentException();
            URI uri=new URI(value);String host=uri.getHost();
            if(!"https".equals(uri.getScheme())||host==null||!provider(host)||uri.getUserInfo()!=null
                ||uri.getFragment()!=null||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getRawPath().isEmpty()
                ||!host.equals(host.toLowerCase(Locale.ROOT)))throw new IllegalArgumentException();
            return uri;
        } catch(Exception invalid) {throw new IllegalArgumentException("Unsupported push provider endpoint");}
    }
    private boolean provider(String host) {
        return host.equals("fcm.googleapis.com")||host.equals("updates.push.services.mozilla.com")
            ||(host.matches("[a-z0-9-]+\\.notify\\.windows\\.com"));
    }
    public InetAddress[] resolve(String host)throws UnknownHostException {
        if(!provider(host))throw new UnknownHostException("Unsupported push provider");
        return checked(InetAddress.getAllByName(host));
    }
    public InetAddress[] checked(InetAddress[] addresses)throws UnknownHostException {
        if(addresses.length==0||Arrays.stream(addresses).anyMatch(a->!publicAddress(a)))
            throw new UnknownHostException("Push provider resolved to a forbidden address");
        return addresses.clone();
    }
    public boolean publicAddress(InetAddress address) {
        if(address.isAnyLocalAddress()||address.isLoopbackAddress()||address.isLinkLocalAddress()
            ||address.isSiteLocalAddress()||address.isMulticastAddress())return false;
        byte[] b=address.getAddress();int a=Byte.toUnsignedInt(b[0]),c=Byte.toUnsignedInt(b[1]);
        if(b.length==4)return a!=0&&a!=10&&a!=127&&a<224&&!(a==100&&c>=64&&c<=127)
            &&!(a==169&&c==254)&&!(a==172&&c>=16&&c<=31)&&!(a==192&&(c==168||c==0||c==88))
            &&!(a==198&&(c==18||c==19||c==51))&&!(a==203&&c==0&&Byte.toUnsignedInt(b[2])==113);
        // Only ordinary global unicast; reject transition/documentation/special ranges.
        return (a&0xe0)==0x20&&!(a==0x20&&c==0x01&&((Byte.toUnsignedInt(b[2])<2)
            ||(Byte.toUnsignedInt(b[2])==0x0d&&Byte.toUnsignedInt(b[3])==0xb8)))
            &&!(a==0x20&&c==0x02)&&!(a==0x3f&&(c==0xfe||c==0xff));
    }
}
