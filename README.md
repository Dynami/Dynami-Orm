# Dynami-Orm

Dynami-Orm is a java annotated Object Relational Mapping engine which supports multiple RDBMS. 

Dynami-Orm is fast, lightweight and without external dependencies giving you full control over databases interactions.

Dynami-Orm uses annotation classes (@IEntity and @IField) for mapping entities and attributes. The library uses the pattern "convention over configuration". Eg. if in @IEntity it isn't specified the attribute "name", the simple class name is used as a table name. Same rules are applied for @IField annotation.

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

The following code defines a database entity called "Person" with four fields, where id is an auto-increment serial and it is also the primary key. Field surname is also indexed field.

```
#!java

import java.util.Date;

import org.dyanmi.orm.IEntity;
import org.dyanmi.orm.IField;

@IEntity
public class Person {
	@IField(pk=true, serial=true)
	private long id;
	
	@IField
	private String name;
	
	@IField(index=true)
	private String surname;
	
	@IField
	private Date birthday;
        
    // getters and setters omitted
}
```
