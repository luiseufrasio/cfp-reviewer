package fish.payara.demo.cfp;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Activates Jakarta REST under {@code /api}.
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
}
