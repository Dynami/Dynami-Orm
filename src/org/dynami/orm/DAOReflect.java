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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.dynami.orm.DAO.IEntity;
import org.dynami.orm.DAO.IField;
import org.dynami.orm.DAO.SqlDialect;

class DAOReflect {
	private static SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
	private static SimpleDateFormat DATE_TYPE = new SimpleDateFormat("yyyyMMdd");
	private static Map<String,Class<?>> cache_classes = new TreeMap<String, Class<?>>();
	private static Map<String,Field[]> cache_fields = new TreeMap<String, Field[]>();
	
	public static final int TRUE = 1;
	public static final int FALSE = 0;
	
	public static Field[] pk(Object obj) throws Exception {
		if(obj == null){
			throw new RuntimeException("Object passed as parameter not valued");
		}
		Class<?> e = getEntity(obj);
		
		List<Field> pks = new ArrayList<Field>();
		Field[] fields = cache_fields.get(e.getName());
		for (int i = 0; i < fields.length; i++) {
			IField f = fields[i].getAnnotation(IField.class);
			if(f != null && f.pk()){
				pks.add(fields[i]);
			}
		}
		return pks.toArray(new Field[pks.size()]);
	}
	
	public static Object[] pkValues(Object entity) throws Exception{
		Field[] pks = pk(entity);
		Object[] res = new Object[pks.length];
		for(int i = 0; i < pks.length; i++){
			res[i] = get(entity, pks[i]);
		}
		return res;
	}
	
	public static String[] pkAsString(Object obj) throws Exception{
		Field[] fields = pk(obj);
		String[] pks = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			pks[i] = getName(fields[i]);
		}
		return pks;
	}
	
	
	public static Field[] fields(Class<?> clazz, boolean withPk) throws Exception {
		getEntity(clazz);
		List<Field> pks = new ArrayList<Field>();
		Field[] fields = cache_fields.get(clazz.getName());
		for (int i = 0; i < fields.length; i++) {
			IField f = fields[i].getAnnotation(IField.class);
			if(f != null){
				if(withPk && f.pk()){
					pks.add(fields[i]);
				} else if(!withPk && f.pk()) {
				} else {
					pks.add(fields[i]);
				}
			}
		}
		Field[] out = new Field[pks.size()];
		pks.toArray(out);
		return out;
	}
	
	public static Field[] fields(Object obj, boolean withPk) throws Exception {
		if(obj == null){
			throw new RuntimeException("Object passed as parameter not valued");
		}
		return fields(getEntity(obj), withPk);
	}
	
	public static Object get(Object obj, Field field) throws Exception{
		if(field.getClass().equals(java.util.Date.class)){
			Date d = (Date)cache_classes.get(obj.getClass().getName()).getMethod(getter(field.getName(), field.getType().equals(Boolean.TYPE)), new Class[]{}).invoke(obj, new Object[]{});
			return Integer.parseInt(DATE_TYPE.format(d));
		} else{
			return cache_classes.get(obj.getClass().getName()).getMethod(getter(field.getName(), field.getType().equals(Boolean.TYPE)), new Class[]{}).invoke(obj, new Object[]{});
		}
	}
	
	public static Object set(Object obj, Field field, Object value) throws Exception{
		if(field.getClass().equals(java.util.Date.class)){
			Date d = DATE_TYPE.parse( String.valueOf(value));
			return cache_classes.get(obj.getClass().getName()).getMethod(setter(field.getName()), new Class[]{field.getType()}).invoke(obj, new Object[]{d});
		} else {
			return cache_classes.get(obj.getClass().getName()).getMethod(setter(field.getName()), new Class[]{field.getType()}).invoke(obj, new Object[]{value});
		}
	}
	
	public static void load(Object obj, String className, Object value) throws Exception{
		Class<?> clazz = cache_classes.get(obj.getClass().getName());
		Method method = clazz.getMethod(loader(className), new Class[]{value.getClass()} );
		method.invoke(obj, new Object[]{value});
	}
	
	private static String getter(String fieldName, boolean isBoolean){
		char[] cs =fieldName.toCharArray();
		cs[0] = Character.toUpperCase(cs[0]);
		return  ((isBoolean)?"is":"get")+ (new String(cs));
	}
	
	private static String setter(String input){
		char[] tmp = input.toCharArray();
		tmp[0] = Character.toUpperCase(tmp[0]);
		return "set".concat(new String(tmp));
	}
	
	private static String loader(String input){
		char[] tmp = input.toCharArray();
		tmp[0] = Character.toUpperCase(tmp[0]);
		return "load".concat(new String(tmp));
	}
	
	public static void logObject(Object input){
		try {
			Class<?> clazz = input.getClass();
			Field[] fields =  cache_fields.get(clazz.getName());
			for (int i = 0; i < fields.length; i++) {
				try{
					Method method =  clazz.getDeclaredMethod(getter(fields[i].getName(), fields[i].getType().equals(Boolean.TYPE)), new Class[]{});
					//log.debug(fields[i].getName()+":\t");
					System.out.print(fields[i].getName()+":\t");
					Object obj = method.invoke(input, new Object[]{});
					if(obj instanceof java.util.Date){
						//log.debug(df.format(obj));
						System.out.println(df.format(obj));
					} else{
						//log.debug(obj);
						System.out.println(obj);
					}
				}catch(Exception w){}
			}
		} catch (Exception e) {
			//log.error("Failed object print", e);
		}
	}
	
	
	static String sqlTableScript(DAO.SqlDialect dialect, Class<?> clazz) throws Exception {
		if(clazz == null){
			throw new RuntimeException("Object passed as parameter not valued");
		}
		BiFunction<IField, Field, String> dataTypeProvider = null;
		switch (dialect) {
		case Sqlite:
			dataTypeProvider = sqliteDataTypeProvider;
			break;
		case MySql:
			dataTypeProvider = mySqlDataTypeProvider;
			break;
		case PostgreSql:
			dataTypeProvider = postgreSqlDataTypeProvider;
			break;
		default:
			break;
		}
		IEntity a = clazz.getAnnotation(IEntity.class);
		if(a == null){
			throw new RuntimeException("Object passed as parameter not a valid table class");
		}
		String tableName = getTableName(clazz);
		String fieldName;
		StringBuilder tableBuffer = new StringBuilder();
		StringBuilder indexBuffer = new StringBuilder();
		StringBuilder pkBuilder = new StringBuilder(" ,PRIMARY KEY(");
		tableBuffer.append("CREATE TABLE IF NOT EXISTS ");
		tableBuffer.append(tableName);
		tableBuffer.append(" ( ");
		Field[] fields = fields(clazz, true);
		IField f = null;
		boolean isPrimaryKeySetted = false;
		for (int i = 0; i < fields.length; i++) {
			f = fields[i].getAnnotation(IField.class);
//			buffer.append("\t");
			fieldName = getName(fields[i]);
			tableBuffer.append(fieldName);
			tableBuffer.append(getType(SqlDialect.Sqlite, f, fields[i], dataTypeProvider));
			if(f.pk()){
				if(isPrimaryKeySetted) {
					pkBuilder.append(", ");
				}
				pkBuilder.append(fieldName);
				isPrimaryKeySetted = true;
			}
//			if(f.serial()){
//				tableBuffer.append(" AUTOINCREMENT ");
//			}
			if(!f.nullable()){
				tableBuffer.append(" NOT NULL ");
			}
			if(f.unique()){
				tableBuffer.append(" UNIQUE ");
			}
			if(!"".equals(f.defaultValue())){
				tableBuffer.append(" DEFAULT "+f.defaultValue());
			}
			
			if(f.index()){
				indexBuffer.append("\nCREATE INDEX IF NOT EXISTS ");
				indexBuffer.append(tableName);
				indexBuffer.append("_");
				indexBuffer.append(fieldName);
				indexBuffer.append("_idx ON ");
				indexBuffer.append(tableName);
				indexBuffer.append("(");
				indexBuffer.append(fieldName);
				indexBuffer.append(");");
			}
			
			if(i < fields.length-1)
			tableBuffer.append(",");
		}
		if(isPrimaryKeySetted) {
			pkBuilder.append(")");
			tableBuffer.append(pkBuilder.toString());
		}
		tableBuffer.append(");");
		return tableBuffer.toString()+indexBuffer.toString();
	}
	

	public static String getTableName(Class<?> e){
		IEntity a = e.getAnnotation(IEntity.class);
		if(a == null || "".equals( a.name()))
			return e.getSimpleName().toLowerCase();
		else 
			return a.name();
	}
	public static String getTableName(Object obj){
		if(obj == null){
			throw new RuntimeException("Object passed as parameter not valued");
		}
		Class<?> e = getEntity(obj);
		IEntity a = e.getAnnotation(IEntity.class);
		if(a == null || "".equals( a.name()))
			return e.getSimpleName().toLowerCase();
		else 
			return a.name();
	}
	
	public static boolean isEntity(Object obj) throws Exception {
		return (getEntity(obj) != null);
	}
	
	public static Field getField(Class<?> clazz, String field) {
		Field[] fields = cache_fields.get(clazz.getName());
		for(Field f:fields) {
			if(f.getName().equals(field)){
				return f;
			}
		}
		return null;
	}
	
	public static Class<?> getEntity(Class<?> clazz){
		if(cache_classes.containsKey(clazz.getName())){
			return cache_classes.get(clazz.getName()); 
		} else {
			IEntity a = clazz.getAnnotation(IEntity.class);
			if(a == null) return null;
			cache_classes.put(clazz.getName(), clazz);
			
			Class<?> c = clazz;
			
			final List<Field> tmp_out = new ArrayList<Field>();
			while(c.getSuperclass() != null) {
				Field[] fields = c.getDeclaredFields();
				for (int i = 0; i < fields.length; i++) {
					IField f = fields[i].getAnnotation(IField.class);
					if(f != null){
						tmp_out.add(fields[i]);
					}
				}
				
				c = c.getSuperclass();
			}
			Field[] out = new Field[tmp_out.size()];
			tmp_out.toArray(out);
			cache_fields.put(clazz.getName(), out);
			return clazz;
		}
	}
	
	public static Class<?> getEntity(final Object obj) {
		if(obj == null){
			throw new RuntimeException("Object passed as parameter not valued");
		}
		
		if(cache_classes.containsKey(obj.getClass().getName())){
			return cache_classes.get(obj.getClass().getName()); 
		} else {
			Class<?> clazz = null;
			if(obj instanceof Class){
				clazz = (Class<?>)obj;
			} else {
				clazz = obj.getClass();
			}
			return getEntity(clazz);
		}
	}
	
	private static String getType(DAO.SqlDialect sqlDialect, IField a, Field field, 
			BiFunction<IField, Field, String> dataTypeProvider) {
		if(a == null || "".equals( a.type())){
			return dataTypeProvider.apply(a, field);
		} else {
			return a.type();
		}
	}
	
	private static BiFunction<IField, Field, String> sqliteDataTypeProvider = new BiFunction<DAO.IField, Field, String>() {
		@Override
		public String apply(IField f, Field field) {
			if(field.getType().equals(String.class)){
				if(f.lenght() != 0){
					return " VARCHAR("+f.lenght()+") ";
				} else {
					return " VARCHAR(255) ";
				}
			} else if(field.getType().equals(java.util.Date.class)){
				return " INTEGER ";				
			} else if(field.getType().equals(double.class)){
				return " REAL ";
			} else if(field.getType().equals(float.class)){
				return " REAL ";
			} else if(field.getType().equals(int.class)){				
				return " INTEGER "+(f.serial()?"AUTOINCREMENT ":"");
			} else if(field.getType().equals(boolean.class)){
				return " INTEGER ";
			} else if(field.getType().equals(long.class)){
				return " INTEGER "+(f.serial()?"AUTOINCREMENT ":"");
			} else {
				return " VARCHAR(50) ";
			}
		}
	};
	
	private static BiFunction<IField, Field, String> mySqlDataTypeProvider = new BiFunction<DAO.IField, Field, String>() {
		public String apply(IField f, Field field) {
			if(field.getType().equals(String.class)){
				if(f.lenght() != 0){
					return " VARCHAR("+f.lenght()+") ";
				} else {
					return " VARCHAR(255) ";
				}
			} else if(field.getType().equals(java.util.Date.class)){
				return " DATETIME ";
			} else if(field.getType().equals(double.class)){
				return " DOUBLE ";
			} else if(field.getType().equals(float.class)){
				return " FLOAT ";
			} else if(field.getType().equals(int.class)){
				return " INTEGER "+(f.serial()?"AUTO_INCREMENT ":"");
			} else if(field.getType().equals(boolean.class)){
				return " BOOLEAN ";
			} else if(field.getType().equals(long.class) ){
				return " BIGINT "+(f.serial()?"AUTO_INCREMENT ":"");
			} else {
				return " VARCHAR(50) ";
			}
		};
	};
	
	private static BiFunction<IField, Field, String> postgreSqlDataTypeProvider = new BiFunction<DAO.IField, Field, String>() {
		public String apply(IField f, Field field) {
			if(field.getType().equals(String.class)){
				if(f.lenght() != 0){
					return " VARCHAR("+f.lenght()+") ";
				} else {
					return " VARCHAR(255) ";
				}
			} else if(field.getType().equals(java.util.Date.class)){
				return " TIMESTAMP ";
			} else if(field.getType().equals(double.class)){
				return " DOUBLE PRECISION ";
			} else if(field.getType().equals(float.class)){
				return " REAL ";
			} else if(field.getType().equals(int.class) && !f.serial()){
				return " INTEGER ";
			} else if(field.getType().equals(int.class) && f.serial()){
				return " SERIAL ";
			} else if(field.getType().equals(short.class) && !f.serial()){
				return " SMALLINT ";
			} else if(field.getType().equals(short.class) && f.serial()){
				return " SMALLSERIAL ";
			} else if(field.getType().equals(boolean.class)){
				return " BOOLEAN ";
			} else if(field.getType().equals(long.class) && !f.serial()){
				return " BIGINT ";
			} else if(field.getType().equals(long.class) && f.serial()){
				return " BIGSERIAL ";
			} else {
				return " VARCHAR(50) ";
			}
		};
	};
	
	
	public static Field getName(Class<?> clazz, String field) throws Exception{
		return getField(clazz, field);
	}
	
	public static String getName(Field field){
		IField f = field.getAnnotation(IField.class);
		if("".equals(f.name())){
			return field.getName();
		} else {
			return f.name();
		}
	}
	

	public static boolean isSerial(Field field) {
		IField f = field.getAnnotation(IField.class);
		return f.serial();
	}
	
	public static boolean isPk(Field field) {
		IField f = field.getAnnotation(IField.class);
		return f.pk();
	}
	
	public static String asString(final Object[] in){
		if(in == null){
			return "null";
		} else {
			StringBuilder buffer = new StringBuilder("[");
			for(Object o:in){
				buffer.append(String.valueOf(o));
				buffer.append(",");
			}
			buffer.append("]");
			return buffer.toString();
		}
	}
}
