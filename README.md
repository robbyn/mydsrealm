# mydsrealm

This is an alternative to the DataSourceRealm that comes with tomcat. MyDataSourceRealm can be used to allow
authentication with either the username, or some other information (typically the e-mail address), and, of course,
a password.

It requires two SQL queries: one to find the user, and one to return the roles of a user.

#### Example:

´´´
  &lt;Realm className="org.tastefuljava.tomcat.MyDataSourceRealm"
        dataSourceName="jdbc/lagalerie" debug="99" localDataSource="true"
        digest="md5" digestEncoding="UTF-8"
        authenticationQuery="SELECT USERNAME FROM users WHERE (USERNAME=:login OR EMAIL=:login) AND PASSWORD=:credentials"
        rolesQuery="SELECT ROLE FROM roles WHERE USERNAME=:username"/&gt;
´´´
