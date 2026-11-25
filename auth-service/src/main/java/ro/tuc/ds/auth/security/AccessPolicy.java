package ro.tuc.ds.auth.security;

import org.springframework.stereotype.Component;
import ro.tuc.ds.auth.entity.Role;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authorization rules applied by the API gateway (through Traefik forwardAuth).
 * <ul>
 *   <li>ADMIN: every operation on /api/users/**, /api/devices/** and /api/monitoring/**</li>
 *   <li>CLIENT: only read its own user (GET /api/users/{ownId}), its own devices (GET /api/devices/user/{ownId})
 *       and the hourly consumption of a device (GET /api/monitoring/devices/{id}/hourly, ownership checked by the Monitoring service)</li>
 * </ul>
 * Anything else is denied. Client paths are matched strictly, so tricks such as ".." or encoded slashes never match.
 */
@Component
public class AccessPolicy {

    private static final String UUID_RE = "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})";
    private static final Pattern OWN_USER = Pattern.compile("^/api/users/" + UUID_RE + "/?$");
    private static final Pattern OWN_DEVICES = Pattern.compile("^/api/devices/user/" + UUID_RE + "/?$");
    /**
     * Hourly consumption of one device. The gateway only checks the shape of the path; the Monitoring service
     * checks that the device really belongs to the caller (X-User-Id) because only it knows the owner.
     */
    private static final Pattern DEVICE_CONSUMPTION = Pattern.compile("^/api/monitoring/devices/" + UUID_RE + "/hourly/?$");

    public boolean isAllowed(AuthenticatedUser user, String method, String uri) {
        String path = stripQuery(uri);
        if (user.role() == Role.ADMIN) {
            return path.equals("/api/users") || path.startsWith("/api/users/")
                    || path.equals("/api/devices") || path.startsWith("/api/devices/")
                    || path.equals("/api/monitoring") || path.startsWith("/api/monitoring/");
        }
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        return matchesOwnId(OWN_USER, path, user) || matchesOwnId(OWN_DEVICES, path, user)
                || DEVICE_CONSUMPTION.matcher(path).matches();
    }

    private static boolean matchesOwnId(Pattern pattern, String path, AuthenticatedUser user) {
        Matcher m = pattern.matcher(path);
        return m.matches() && m.group(1).equalsIgnoreCase(user.userId().toString());
    }

    private static String stripQuery(String uri) {
        if (uri == null) {
            return "";
        }
        int q = uri.indexOf('?');
        return q >= 0 ? uri.substring(0, q) : uri;
    }
}
