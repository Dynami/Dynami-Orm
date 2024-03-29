/*
 * Copyright 2017 Alessandro Atria - a.atria@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dynami.orm;

import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * DAO is a lightweight and basic ORM (Object Relational Mapping) and expose primary methods to handle data in RDBMS.
 * DAO generates standard SQL, specific instructions can be executed passing sql statements and using DAO only as pojo-sql mapping engine.
 *  
 * @author Alessandro Atria - a.atria@gmail.com
 *
 */
public enum DAO {
	$;
	
	//public static final DAO $ = new DAO();
//	public static final DAO newInstance() {
//		return new DAO();
//	}
//	private DAO() {}
//	
	public enum SqlDialect {Sqlite, MySql, PostgreSql};
	
	private DataSource ds;
	private SqlDialect sqlDialect;
	private Release release;
	private static final Map<String, Map<String, Object>> cached_objects = new TreeMap<>(); 
	private static final List<Class<?>> cached_classes = new ArrayList<>();
	
	public void setup(SqlDialect sqlDialect, DataSource ds) {
		setup(sqlDialect, ds, c->{});
	}
	
	public void setup(SqlDialect sqlDialect, DataSource ds, Release release) {
		this.ds = ds;
		this.sqlDialect = sqlDialect;
		this.release = release;
	}
	
	/**
	 * Get single instance identified by primary keys
	 * @param clazz
	 * @param primaryKey get in the same order as defined in class
	 * @return valued object
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T load(Class<T> clazz, Object... primaryKey) throws Exception {
		boolean cachable = checkEntityTable(clazz);
		if(cachable && cached_objects.get(clazz.getName()) != null){
			T res = (T)cached_objects.get(clazz.getName()).get(DAOReflect.asString( primaryKey ));
			if(res != null){
				return res;
			}
		}
		try{
			Object entity = clazz.getDeclaredConstructor().newInstance();
			Field[] pk = DAOReflect.pk(entity);
			for (int i = 0; i < pk.length; i++) {
				DAOReflect.set(entity, pk[i], primaryKey[i]);
			}
			Object res = get(entity);
			if(cachable && res != null){
				if(cached_objects.get(clazz.getName()) == null){
					cached_objects.put(clazz.getName(), new ConcurrentSkipListMap<String, Object>());
				}
				cached_objects.get(clazz.getName()).put(DAOReflect.asString( primaryKey ), res);
			}
			return (T) res;
		}catch(Exception e){
			System.err.println("Error on"+ clazz.getName());
			e.printStackTrace();
			throw e;
		}
	}
	
	
	public <T> T get(T entity) throws Exception {
		checkEntityTable(entity.getClass());
		PreparedStatement pstmt = null;
		ResultSet res = null;
		Connection conn = ds.getConnection();
		try {
			pstmt = SqlUtils.sqlSelect(conn, entity);
			
			res = pstmt.executeQuery();
			Field[] fields = DAOReflect.fields(entity, true); // entity.getClass().getDeclaredFields();
			
			if(res.next()){
				for (int i = 0; i < fields.length; i++) {
					setField(fields[i], entity, res);
					IField f = fields[i].getAnnotation(IField.class);
					if(f != null && !f.fk().equals(Object.class)){
						Object o = load(f.fk() , DAOReflect.get(entity, fields[i]));
						DAOReflect.load(entity, o.getClass().getSimpleName(), o);
					}
				}
				return entity;
			} else {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			DAOReflect.logObject(entity);
			//log.error("get", e);
			throw e;
		} finally {
			SqlUtils.closeAll(pstmt, res);
			release.accept(conn);
		}
	}
	
	
	/**
	 * Update entity passed as parameter
	 * @param entity uses the primary field values in where condition to identify records
	 * @return the number of records updated 
	 * @throws Exception
	 */
	public <T> int update(T entity) throws Exception {
		boolean cachable = checkEntityTable(entity.getClass());
		if(cachable && cached_objects.get(entity.getClass().getName()) != null){
			cached_objects.get(entity.getClass().getName()).remove(DAOReflect.asString( DAOReflect.pkValues(entity) ));
		}
		Connection conn = ds.getConnection();
		PreparedStatement pstmt = null;
		try{
			pstmt = SqlUtils.sqlUpdate(conn, entity);
			return pstmt.executeUpdate();
		} catch (Exception e) {
			DAOReflect.logObject(entity);
			//log.error("update", e);
			throw e;
		} finally {
			SqlUtils.closeAll(pstmt);
			release.accept(conn);
		}
	}
	
	/**
	 * 
	 * @param entity update entity passed as parameter excluding defined fields
	 * @param exclude list of fields to be excluded
	 * @return the number of records updated
	 * @throws Exception
	 */
	public <T> int update(T entity, String...exclude) throws Exception {
		boolean cachable = checkEntityTable(entity.getClass());
		if(cachable && cached_objects.get(entity.getClass().getName()) != null){
			cached_objects.get(entity.getClass().getName()).remove(DAOReflect.asString( DAOReflect.pkValues(entity) ));
		}
		Connection conn = ds.getConnection();
		PreparedStatement pstmt = null;
		try{
			pstmt = SqlUtils.sqlUpdate(conn, entity, exclude);
			return pstmt.executeUpdate();
		} catch (Exception e) {
			DAOReflect.logObject(entity);
			//log.error("update", e);
			throw e;
		} finally {
			SqlUtils.closeAll(pstmt);
			release.accept(conn);
		}
	}
	
	/**
	 * Deletes passed entity defined by valued primary keys
	 * @param entity primary keys field have to be valued to avoid undesired deletions
	 * @return the number of records deleted
	 * @throws Exception
	 */
	public <T> int delete(T entity) throws Exception {
		boolean cachable = checkEntityTable(entity.getClass());
		if(cachable && cached_objects.get(entity.getClass().getName()) != null){
			cached_objects.get(entity.getClass().getName()).remove(DAOReflect.asString( DAOReflect.pkValues(entity) ));
		}
		Connection conn = ds.getConnection();
		PreparedStatement pstmt = null;
		try{
			pstmt = SqlUtils.sqlDelete(conn, entity);
			return pstmt.executeUpdate();
		} catch (Exception e) {
			DAOReflect.logObject(entity);
			//log.error("update", e);
			throw e;
		} finally {
			SqlUtils.closeAll(pstmt);
			release.accept(conn);
		}
	}
	
	/**
	 * 
	 * @param entity
	 * @return
	 * @throws Exception
	 */
	public int insert(Object entity) throws Exception {
		if(entity == null) return -1;
		checkEntityTable(entity.getClass());
		PreparedStatement pstmt = null;
		Connection conn = ds.getConnection();
		try{
			pstmt = SqlUtils.sqlInsert(conn, entity);
			return pstmt.executeUpdate();
		} catch (Exception e) {
			DAOReflect.logObject(entity);
			throw e;
		} finally {
			SqlUtils.closeAll(pstmt);
			release.accept(conn);
		}
	}
	
	public <T> int delete(Criteria<T> criteria) throws Exception {
		boolean cachable = checkEntityTable(criteria.clazz);
		if(cachable){
			cached_objects.remove(criteria.clazz.getName());
		}
		int result = 0;
		Connection conn = ds.getConnection();
		PreparedStatement pstmt = null;
		try {
			pstmt = SqlUtils.sqlDeleteByCriteria(conn, criteria);
			result = pstmt.executeUpdate();
			return result;
		} catch (Exception e) {
			DAOReflect.logObject(criteria);
			throw e;
		} finally {
			SqlUtils.closeAll(pstmt);
			release.accept(conn);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> List<T> select(Criteria<T> criteria) throws Exception {
		checkEntityTable( criteria.clazz);
		PreparedStatement pstmt = null;
		ResultSet res = null;
		List<T> result = new ArrayList<T>();
		Connection conn = ds.getConnection();
		try {
			pstmt = SqlUtils.sqlSelectByCriteria(conn, criteria);
			Object entity = criteria.getIEntity();
			res = pstmt.executeQuery();
			Field[] fields = DAOReflect.fields(entity, true); //entity.getClass().getDeclaredFields();
			Object obj = null;
			while(res.next()){
				obj = entity.getClass().getDeclaredConstructor().newInstance();
				for (int i = 0; i < fields.length; i++) {
					//System.out.println(fields[i].getName());
					setField(fields[i], obj, res);
					IField f = fields[i].getAnnotation(IField.class);
					if(f != null && !f.fk().equals(Object.class)){
						Object o = load(f.fk() , DAOReflect.get(obj, fields[i]));
						DAOReflect.load(obj, o.getClass().getSimpleName(), o);
					}
				}
				result.add((T)obj);
			}
			return result;
		} catch (Exception e) {
			DAOReflect.logObject(criteria);
			throw e;
		} finally {
			SqlUtils.closeAll(pstmt, res);
			release.accept(conn);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T>  T selectFirst(Criteria<T> criteria) throws Exception {
		checkEntityTable(criteria.clazz);
		PreparedStatement pstmt = null;
		ResultSet res = null;
		Connection conn = ds.getConnection();
//		List<T> result = new ArrayList<T>();
		try {
			pstmt = SqlUtils.sqlSelectByCriteria(conn, criteria);
			Object entity = criteria.getIEntity();
			res = pstmt.executeQuery();
			Field[] fields = DAOReflect.fields(entity, true); //entity.getClass().getDeclaredFields();
			Object obj = null;
			if(res.next()){
				obj = entity.getClass().getDeclaredConstructor().newInstance();
				for (int i = 0; i < fields.length; i++) {
					//System.out.println(fields[i].getName());
					setField(fields[i], obj, res);
					IField f = fields[i].getAnnotation(IField.class);
					if(f != null && !f.fk().equals(Object.class)){
						Object o = load(f.fk() , DAOReflect.get(obj, fields[i]));
						DAOReflect.load(obj, o.getClass().getSimpleName(), o);
					}
				}
				//result.add((T)obj);
			}
			return (T)obj;
		} catch (Exception e) {
			DAOReflect.logObject(criteria);
			throw e;
		} finally {
			SqlUtils.closeAll(pstmt, res);
			release.accept(conn);
		}
	}
	
	public <T> int select(Consumer<T> fetch, Class<T> clazz, String sql, Object... values) throws Exception {
		if(ds == null) throw new Exception("Datasource not settled up");
		Connection conn = ds.getConnection();
		PreparedStatement pstmt = null;
		ResultSet res = null;
		int processed = 0;
		try {
			pstmt = conn.prepareStatement(sql);
			int idx = 1;
			for(Object v:values){
				pstmt.setObject(idx++, v);
			}
			
			res = pstmt.executeQuery();
			T obj = clazz.getDeclaredConstructor().newInstance();
			Field[] fields = DAOReflect.fields(obj, true); //entity.getClass().getDeclaredFields();
			while(res.next()){
				obj = clazz.getDeclaredConstructor().newInstance();
				for (int i = 0; i < fields.length; i++) {
					setField(fields[i], obj, res);
				}
				fetch.accept((T)obj);
				processed++;
			}
			return processed;
		} catch (Exception e) {
			//DAOUtils.logObject(criteria);
			throw e;
		} finally {
			SqlUtils.closeAll(pstmt, res);
			release.accept(conn);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> int select(Criteria<T> criteria, Consumer<T> fetch) throws Exception {
		checkEntityTable(criteria.clazz);
		Connection conn = ds.getConnection();
		PreparedStatement pstmt = null;
		ResultSet res = null;
		try {
			pstmt = SqlUtils.sqlSelectByCriteria(conn, criteria);
			Object entity = criteria.getIEntity();
			res = pstmt.executeQuery();
			Field[] fields = DAOReflect.fields(entity, true); //entity.getClass().getDeclaredFields();
			Object obj = entity.getClass().getDeclaredConstructor().newInstance();
			int processedRows = 0;
			while(res.next()){
				for (int i = 0; i < fields.length; i++) {
					setField(fields[i], obj, res);
					final IField f = fields[i].getAnnotation(IField.class);
					if(f != null && !f.fk().equals(Object.class)){
						Object o = load(f.fk() , DAOReflect.get(obj, fields[i]));
						DAOReflect.load(obj, o.getClass().getSimpleName(), o);
					}
				}
				fetch.accept((T)obj);
				processedRows++;
			}
			return processedRows;
		} catch (Exception e) {
			DAOReflect.logObject(criteria);
			throw e;
		} finally {
			SqlUtils.closeAll(pstmt, res);
			release.accept(conn);
		}
	}
	
	
	
	public <T extends Number> T number(Class<T> clazz, String sql, Object...values) throws Exception{
		if(ds == null) throw new Exception("Datasource not settled up");
		Connection conn = ds.getConnection();
		PreparedStatement pstmt = null;
		ResultSet res = null;
		try {
			pstmt = conn.prepareStatement(sql);
			int idx = 1;
			for(Object v:values){
				pstmt.setObject(idx++, v);
			}
			res = pstmt.executeQuery();
			if(res.next()){
				if(clazz.equals(Double.class)){
					return clazz.getConstructor(double.class).newInstance(res.getDouble(1));
				} else if(clazz.equals(Float.class)){
					return clazz.getConstructor(float.class).newInstance(res.getFloat(1));
				} else if(clazz.equals(Integer.class)){
					return clazz.getConstructor(int.class).newInstance(res.getInt(1));
				} else if(clazz.equals(Long.class)){
					return clazz.getConstructor(long.class).newInstance(res.getLong(1));
				} else if(clazz.equals(Short.class)){
					return clazz.getConstructor(short.class).newInstance(res.getShort(1));
				} else {
					return null;
				}
			} else {
				return null;
			}
		} catch (Exception e) {
			throw e;
		} finally {
			SqlUtils.closeAll(pstmt, res);
			release.accept(conn);
		}
	}
	
	public <T extends Number> List<T> numbers(Class<T> clazz, String sql, Object...values) throws Exception{
		if(ds == null) throw new Exception("Datasource not settled up");
		Connection conn = ds.getConnection();
		PreparedStatement pstmt = null;
		ResultSet res = null;
		List<T> output = new ArrayList<>();
		try {
			pstmt = conn.prepareStatement(sql);
			int idx = 1;
			for(Object v:values){
				pstmt.setObject(idx++, v);
			}
			res = pstmt.executeQuery();
			while(res.next()){
				if(clazz.equals(Double.class)){
					output.add( clazz.getConstructor(double.class).newInstance(res.getDouble(1)) );
				} else if(clazz.equals(Float.class)){
					output.add( clazz.getConstructor(float.class).newInstance(res.getFloat(1)) );
				} else if(clazz.equals(Integer.class)){
					output.add( clazz.getConstructor(int.class).newInstance(res.getInt(1)) );
				} else if(clazz.equals(Long.class)){
					output.add( clazz.getConstructor(long.class).newInstance(res.getLong(1)) );
				} else if(clazz.equals(Short.class)){
					output.add( clazz.getConstructor(short.class).newInstance(res.getShort(1)) );
				} else if(clazz.equals(Boolean.class)){
					output.add( clazz.getConstructor(boolean.class).newInstance(res.getBoolean(1)) );
				} else {
					return null;
				}
			}
			return output;
		} catch (Exception e) {
			throw e;
		} finally {
			SqlUtils.closeAll(pstmt, res);
			release.accept(conn);
		}
	}
	
	public <T> List<T> select(Class<T> clazz, String sql, Object...values) throws Exception {
		if(ds == null) throw new Exception("Datasource not settled up");
		Connection conn = ds.getConnection();
		PreparedStatement pstmt = null;
		ResultSet res = null;
		List<T> result = new ArrayList<T>();
		try {
			pstmt = conn.prepareStatement(sql);
			int idx = 1;
			for(Object v:values){
				pstmt.setObject(idx++, v);
			}
			
			res = pstmt.executeQuery();
			T obj = clazz.getDeclaredConstructor().newInstance();
			Field[] fields = DAOReflect.fields(obj, true); //entity.getClass().getDeclaredFields();
			while(res.next()){
				obj = clazz.getDeclaredConstructor().newInstance();
				for (int i = 0; i < fields.length; i++) {
					setField(fields[i], obj, res);
				}
				result.add((T)obj);
			}
			return result;
		} catch (Exception e) {
			//DAOUtils.logObject(criteria);
			throw e;
		} finally {
			SqlUtils.closeAll(pstmt, res);
			release.accept(conn);
		}
	}
	
	public <T> T selectFirst(Class<T> clazz, String sql, Object...values) throws Exception {
		if(ds == null) throw new Exception("Datasource not settled up");
		Connection conn = ds.getConnection();
		PreparedStatement pstmt = null;
		ResultSet res = null;
		try {
			pstmt = conn.prepareStatement(sql);
			int idx = 1;
			for(Object v:values){
				pstmt.setObject(idx++, v);
			}
			
			res = pstmt.executeQuery();
			T obj = clazz.getDeclaredConstructor().newInstance();
			Field[] fields = DAOReflect.fields(obj, true); //entity.getClass().getDeclaredFields();
			if(res.next()){
				obj = clazz.getDeclaredConstructor().newInstance();
				for (int i = 0; i < fields.length; i++) {
					setField(fields[i], obj, res);
				}
				return obj;
			}
			return null;
		} catch (Exception e) {
			//DAOUtils.logObject(criteria);
			throw e;
		} finally {
			SqlUtils.closeAll(pstmt, res);
			release.accept(conn);
		}
	}
	
	public boolean save(Object entity)throws Exception{
		checkEntityTable(entity.getClass());
		if(update(entity) == 0){
			insert(entity);
			return true;
		}
		return false;
	}
	
	
	public <T> boolean insertIfNotExist(T entity, Criteria<? extends T> criteria) throws Exception{
		checkEntityTable(entity.getClass());
		if(!exists(criteria)){
			insert(entity);
			return true;
		} else {
			return false;
		}
	}
	
	public <T> boolean exists(Criteria<T> criteria) throws Exception {
		checkEntityTable(criteria.clazz);
		return selectFirst(criteria) != null;
	}
	
	public void executeNativeSQL(String sql) throws Exception {
		Statement pstmt = null;
		Connection conn = ds.getConnection();
		try{
			pstmt = conn.createStatement();
			String[] statements = sql.split(";");
			for(String s:statements){
				pstmt.addBatch(s);
			}
			pstmt.executeBatch();
		}catch(Exception e){
			throw e;
		} finally{
			 SqlUtils.closeAll(pstmt);
			 release.accept(conn);
		}
	}
	
	private boolean checkEntityTable(Class<?> entity) throws Exception {
		if(ds == null) throw new Exception("Datasource not settled up");
		
		if(!DAOReflect.isEntity(entity)){
			throw new Exception("Object passed as parameter is not an Entity");
		}
		if(!cached_classes.contains(entity)){
			cached_classes.add(entity);
			executeNativeSQL(DAOReflect.sqlTableScript(sqlDialect, DAOReflect.getEntity(entity)));
		}
		IEntity a = entity.getAnnotation(IEntity.class);
		if(a != null){
			return a.cache();
		}
		return false;
	}
	
	public void executeNativeSQL(String sql, Object...values) throws Exception {
		if(ds == null) throw new Exception("Datasource not settled up");
		Connection conn = ds.getConnection();
		PreparedStatement pstmt = null;
		try{
			pstmt = conn.prepareStatement(sql);
			for(int i = 1; i <= values.length; i++){
				pstmt.setObject(i, values[i-1]);
			}
			pstmt.execute();
		}catch(Exception e){
			throw e;
		} finally{
			 SqlUtils.closeAll(pstmt);
			 release.accept(conn);
		}
	}
	
	private static void setField(final Field field, final Object entity, final ResultSet res) throws Exception{
		if(field.getType().equals(String.class)){
			DAOReflect.set(entity, field, res.getString(DAOReflect.getName(field)));
		} else if(field.getType().equals(java.util.Date.class)){
			DAOReflect.set(entity, field, res.getDate(DAOReflect.getName(field)));
		} else if(field.getType().equals(double.class)){
			DAOReflect.set(entity, field, res.getDouble(DAOReflect.getName(field)));
		} else if(field.getType().equals(float.class)){
			DAOReflect.set(entity, field, res.getFloat(DAOReflect.getName(field)));
		} else if(field.getType().equals(boolean.class)){
			DAOReflect.set(entity, field, res.getBoolean(DAOReflect.getName(field)));
		} else if(field.getType().equals(int.class)){
			DAOReflect.set(entity, field, res.getInt(DAOReflect.getName(field)));
		} else if(field.getType().equals(short.class)){
			DAOReflect.set(entity, field, res.getShort(DAOReflect.getName(field)));
		} else if(field.getType().equals(long.class)){
			DAOReflect.set(entity, field, res.getLong(DAOReflect.getName(field)));
		} else {
			System.out.println("\t-->"+DAOReflect.getName(field));
			DAOReflect.set(entity, field, res.getString(DAOReflect.getName(field)));
		}
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface IField {
		
		/**
		 * Indicates field as Primary key
		 * @return
		 */
		boolean pk() default false;
		
		/**
		 * Generate serial number for field
		 * @return
		 */
		boolean serial() default false;
		
		/**
		 * Indicates whether the field can be settled with a null value
		 * @return
		 */
		boolean nullable() default true;
		
		/**
		 * Indicates whether the field can be duplicated
		 * @return
		 */
		boolean unique() default false;
		
		/**
		 * Defines column name, if isn't specified it is equal to field name 
		 * @return
		 */
		String name() default "";
		
		/**
		 * Override default "java type"/"sqlite type" mapping
		 * @return
		 */
		String type() default "";
		
		String defaultValue() default ""; 
		
		int lenght() default 0;
		
		/**
		 * Refers to one to many relation object
		 * @return
		 */
		Class<?> fk() default Object.class;
		
		boolean index() default false;
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface IEntity {
		String name() default "";
		/**
		 * Indicates whether object can be cached or not.
		 * @return
		 */
		boolean cache() default false;
	}
	
	@FunctionalInterface
	public interface Release extends Consumer<Connection> {
		@Override
		default void accept(final Connection elem) {
			try {
				acceptThrows(elem);
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}
		void acceptThrows(Connection elem) throws Exception;
	}
	
	public static class SingleConnectionDataSource implements DataSource {
		private PrintWriter out = new PrintWriter(System.out);
		private final Connection conn;
		
		public SingleConnectionDataSource(Connection conn) {
			this.conn = conn;
		}
		
		@Override
		public PrintWriter getLogWriter() throws SQLException {
			return out;
		}

		@Override
		public void setLogWriter(PrintWriter out) throws SQLException {
			this.out = out;
		}

		@Override
		public void setLoginTimeout(int seconds) throws SQLException {}

		@Override
		public int getLoginTimeout() throws SQLException { return 0; }

		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException { return null; }

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException { return null; }

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {return false;}

		@Override
		public Connection getConnection() throws SQLException {
			return conn;
		}

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			return conn;
		}
	}
}
