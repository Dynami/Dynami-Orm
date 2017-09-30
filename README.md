# Dynami-Orm

Dynami-Orm is a basic Object Relational Mapping which exposes primary methods to handle data in RDBMS. It is lightweight, fast and without external dependencies. 
Dynami-Orm generates standard SQL, but if necessary specific native-instructions can be passed as method parameters.

Dynami-Orm uses annotation classes (@IEntity and @IField) for mapping entities and attributes. The library uses the pattern "convention over configuration". Eg. if in @IEntity it isn't specified the attribute "name", the simple class name is used as a table name. Same rules are applied for @IField annotation.

Dynami-Orm can self configure database creating tables for the specified RDBMS if they don't exist using the @IEntity and @IField attributes. 

Dynami-Orm is currently tested on Sqlite3 and MySql. 

Dynami-Orm main class is DAO.$, which is a ready-to-use singleton class. It accepts a javax.sql.DataSource as parameter. The following example uses Hikari library to handle DataSource with Sqlite3.

```
#!java
DAO.$.setup()
```


```
#!java

File databaseFile = new File("test.db");
HikariConfig hikariConfig = new HikariConfig();
hikariConfig.setPoolName("SQLiteConnectionPool");
hikariConfig.setDriverClassName("org.sqlite.JDBC");
hikariConfig.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());

HikariDataSource ds = new HikariDataSource(hikariConfig);
DAO.$.setup(DAO.SqlDialect.Sqlite, ds);
```

The following code defines a database entity called "Person" with four fields, where id is an auto-increment serial and it is also the primary key. Field surname is also indexed field.

```
#!java

import java.util.Date;

import org.dynami.orm.DAO.IEntity;
import org.dynami.orm.DAO.IField;

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
