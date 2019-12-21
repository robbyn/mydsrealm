# mydsrealm

This is an alternative to the DataSourceRealm that comes with tomcat. MyDataSourceRealm can be used to allow
authentication with either the username, or some other information (typically the e-mail address), and, of course,
a password (or digest).

It requires two SQL queries: one to find the user, and one to return the roles of a user.

#### Example:

```xml
    <Realm className="org.tastefuljava.tomcat.MyDataSourceRealm"
        dataSourceName="jdbc/lagalerie" debug="99" localDataSource="true"
        digest="md5" digestEncoding="UTF-8"
        authenticationQuery="SELECT USERNAME FROM users WHERE (USERNAME=:login OR EMAIL=:login) AND PASSWORD_HASH=:credentials"
        rolesQuery="SELECT ROLE FROM roles WHERE USERNAME=:username">
        <CredentialHandler className="org.apache.catalina.realm.MessageDigestCredentialHandler"
            algorithm="MD5" encoding="UTF-8" iterations="1" saltLength="0" />
    </Realm>
```

The project also contains a valve that forces a basic authentication in the presence of an Authorization header. The
Web App may be configured with FORM authentication, but the form authentication will by bypassed if the Authorization
header contains valid credentials.

To use it, just add the following line in your META-INF/context.xml:

```xml
    <Valve className="org.tastefuljava.tomcat.AutoBasicValve" />
```

Note that by default, the valve will use the UTF-8 encoding. To use another encoding, just add the "encoding" attribute:

```xml
    <Valve className="org.tastefuljava.tomcat.AutoBasicValve" encoding="ISO-8859-1" />
```