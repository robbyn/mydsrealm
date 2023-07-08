package org.tastefuljava.tomcat;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.apache.naming.ContextBindings;

public class MyDataSourceRealm extends RealmBase {
    protected static final String NAME = "MyDataSourceRealm";
    protected static final String INFO =
            "org.tastefuljava.tomcat.MyDataSourceRealm/1.0";

    protected String dataSourceName = null;
    protected boolean localDataSource = false;

    protected String authenticationQuery;
    protected String[] authenticationNames;
    protected String rolesQuery;
    protected String[] rolesNames;

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public boolean getLocalDataSource() {
        return localDataSource;
    }

    public void setLocalDataSource(boolean localDataSource) {
        this.localDataSource = localDataSource;
    }

    public String getAuthenticationQuery() {
        return authenticationQuery;
    }

    public void setAuthenticationQuery(String authenticationQuery) {
        QueryParser parser = new QueryParser();
        parser.parse(authenticationQuery);
        this.authenticationQuery = parser.getQuery();
        authenticationNames = parser.getNames();
    }

    public String getRolesQuery() {
        return rolesQuery;
    }

    public void setRolesQuery(String rolesQuery) {
        QueryParser parser = new QueryParser();
        parser.parse(rolesQuery);
        this.rolesQuery = parser.getQuery();
        rolesNames = parser.getNames();
    }

    @Override
    public synchronized Principal authenticate(String login,
            String credentials) {
        // No user or no credentials
        // Can't possibly authenticate, don't bother the database then
        if (login == null || credentials == null) {
            return null;
        }

        try (Connection cnt = open()) {
            String toValidate = getCredentialHandler().mutate(credentials);
            Object userId = getUserId(cnt, login, toValidate);

            if (userId != null) {
                if (containerLog.isTraceEnabled())
                    containerLog.trace(sm.getString(
                            "jdbcRealm.authenticateSuccess", userId));
            } else {
                if (containerLog.isTraceEnabled())
                    containerLog.trace(sm.getString(
                            "jdbcRealm.authenticateFailure", login));
                return null;
            }

            List<String> roles = getUserRoles(cnt, userId);

            // Create and return a suitable Principal for this user
            return new GenericPrincipal(userId.toString(), credentials, roles);
        } catch (SQLException ex) {
            containerLog.error(
                    sm.getString("dataSourceRealm.getPassword.exception",
                                 login));
            return null;
        }
    }

    @Override
    protected String getPassword(String username) {
        return null;
    }

    @Override
    protected Principal getPrincipal(String username) {
        return null;
    }

    protected Connection open() {
        try {
            Context context;
            if (localDataSource) {
                context = ContextBindings.getClassLoader();
                context = (Context) context.lookup("comp/env");
            } else {
                context = getServer().getGlobalNamingContext();
            }
            DataSource dataSource = (DataSource)context.lookup(dataSourceName);
	    return dataSource.getConnection();
        } catch (SQLException | NamingException e) {
            // Log the problem for posterity
            containerLog.error(sm.getString("dataSourceRealm.exception"), e);
        }  
        return null;
    }

    protected Object getUserId(Connection cnt, String login,
            String credentials) throws SQLException {
        Map<String,Object> parms = new HashMap<>();
        parms.put("login", login);
        parms.put("credentials", credentials);
        try (PreparedStatement stmt = prepareStatement(cnt, authenticationQuery,
                authenticationNames, parms)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Object result = rs.getObject(1);
                if (rs.next()) {
                    // ambiguous login
                    return null;
                }
                return result;
            }
            return null;
        }
    }

    protected List<String> getUserRoles(Connection cnt, Object userid)
            throws SQLException {
        Map<String,Object> parms = new HashMap<>();
        parms.put("username", userid);
        try (PreparedStatement stmt = prepareStatement(cnt, rolesQuery,
                rolesNames, parms)) {
            ResultSet rs = stmt.executeQuery();
            List<String> result = new ArrayList<>();
            while (rs.next()) {
                result.add(rs.getString(1));
            }
            return result;
        }
    }

    protected PreparedStatement prepareStatement(Connection cnt, String query,
            String[] names, Map<String,Object> parms) throws SQLException {
        boolean ok = false;
        PreparedStatement stmt = cnt.prepareStatement(query);
        try {
            int i = 0;
            for (String parmName: names) {
                Object value = parms.get(parmName);
                stmt.setObject(++i, value);
            }
            ok = true;
        } finally {
            if (!ok) {
                stmt.close();
            }
        }
        return stmt;
    }
}
