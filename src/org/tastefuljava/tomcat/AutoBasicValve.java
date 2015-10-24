package org.tastefuljava.tomcat;

import java.io.IOException;
import java.security.Principal;
import javax.servlet.ServletException;
import org.apache.catalina.Realm;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

public class AutoBasicValve extends ValveBase {
    private static final String BASIC_PREFIX = "basic ";

    private String encoding = "UTF-8";

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Override
    public void invoke(Request request, Response response)
            throws IOException, ServletException {
        Principal principal = request.getUserPrincipal();
        Realm realm = getContainer().getRealm();
        if (principal != null) {
            if (containerLog.isDebugEnabled()) {
                containerLog.debug(
                        "Already authenticated as: " + principal.getName());
            }
        } else if (realm == null) {
            if (containerLog.isDebugEnabled()) {
                containerLog.debug("No realm configured");
            }
        } else {
            String auth = request.getHeader("authorization");
            if (auth != null) {
                if (auth.toLowerCase().startsWith(BASIC_PREFIX)) {
                    auth = auth.substring(BASIC_PREFIX.length());
                    byte[] bytes = Base64.decode(auth);
                    auth = new String(bytes, encoding);
                    int ix = auth.indexOf(':');
                    if (ix >= 0) {
                        String username = auth.substring(0, ix);
                        String password = auth.substring(ix+1);
                        principal = realm.authenticate(username, password);
                        if (principal == null) {
                            containerLog.warn(
                                    "Could not authenticate " + username);
                        } else {
                            containerLog.info(
                                    "Authenticated as " + principal.getName());
                            request.setAuthType("BASIC");
                            request.setUserPrincipal(principal);
                        }
                    }
                }
            }
        }
        getNext().invoke(request, response);
    }
}
