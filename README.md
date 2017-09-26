# Dynami-Orm

Dynami-Orm is a java annotated Object Relational Mapping engine which supports multiple RDBMS. 
Dynami-Orm is fast, lightweight and without external dependencies giving you full control over databases interactions.

Dynami-Orm uses annotation classes (@IEntity and @IField) for persisting models and attributes. The library uses the pattern "convention over configuration". Eg. if in @IEntity it isn't specified the attribute "name", the simple class name is used as a table name. Same rules are applied for @IField annotation.

DAO.$ is a ready-to-use singleton class, which doesn't require any configurations because javax.sql.DataSource is passed as parameter. The following example uses Hikari library to handle DataSource with Sqlite3.

```
#!java
File databaseFile = new File("test.db");
HikariConfig hikariConfig = new HikariConfig();
hikariConfig.setPoolName("SQLiteConnectionPool");
hikariConfig.setDriverClassName("org.sqlite.JDBC");
hikariConfig.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());

DAO.$.setUp(new HikariDataSource(hikariConfig));
```
