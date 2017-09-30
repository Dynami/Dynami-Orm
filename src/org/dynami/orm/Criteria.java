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
import java.util.ArrayList;
import java.util.List;

public class Criteria<T> {
	
	protected final Class<?> clazz;
	protected final List<Where> criteria = new ArrayList<Where>();
	protected Object entity;
	protected String[] distinct = new String[0];
	protected String[] orderBy = new String[0];
	protected String[] groupBy = new String[0];
	protected int rows = -1;
	
	public Criteria(Class<T> t){
		this.clazz = DAOReflect.getEntity(t);
		try {
			this.entity = this.clazz.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public Object getIEntity(){
		return this.entity;
	}
	
	protected List<Where> getCriteria(){
		return this.criteria;
	}
	
	public Criteria<T> distinct(String...fields){
		distinct = fields;
		return this;
	}
	
	public Criteria<T> andEquals(String field, Object value){
		try {
			criteria.add(new Where(Where.AND, DAOReflect.getName(clazz, field), Where.EQ, new Object[]{value}));
		} catch (Exception e) {
			//log.error("andEquals() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> orEquals(String field, Object value){
		try {
			criteria.add(new Where(Where.OR, DAOReflect.getName(clazz, field), Where.EQ, new Object[]{value}));
		} catch (Exception e) {
			//log.error("orEquals() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> andNotEquals(String field, Object value){
		try {
			criteria.add(new Where(Where.AND, DAOReflect.getName(clazz, field), Where.NEQ, new Object[]{value}));
		} catch (Exception e) {
			//log.error("andNotEquals() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> orNotEquals(String field, Object value){
		try {
			criteria.add(new Where(Where.OR, DAOReflect.getName(clazz, field), Where.NEQ, new Object[]{value}));
		} catch (Exception e) {
			//log.error("orNotEquals() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	
	public Criteria<T> andGreaterThan(String field, Object value){
		try {
			criteria.add(new Where(Where.AND, DAOReflect.getName(clazz, field), Where.GT, new Object[]{value}));
		} catch (Exception e) {
			//log.error("andGreaterThan() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> orGreaterThan(String field, Object value){
		try {
			criteria.add(new Where(Where.OR, DAOReflect.getName(clazz, field), Where.GT, new Object[]{value}));
		} catch (Exception e) {
			//log.error("orGreaterThan() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> andEqualsGreaterThan(String field, Object value){
		try {
			criteria.add(new Where( Where.AND, DAOReflect.getName(clazz, field), Where.EGT, new Object[]{value}));
		} catch (Exception e) {
			//log.error("andEqualsGreaterThan() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> orEqualsGreaterThan(String field, Object value){
		try {
			criteria.add(new Where( Where.OR, DAOReflect.getName(clazz, field), Where.EGT, new Object[]{value}));
		} catch (Exception e) {
			//log.error("orEqualsGreaterThan() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> andLowerThan(String field, Object value){
		try {
			criteria.add(new Where(Where.AND, DAOReflect.getName(clazz, field), Where.LT, new Object[]{value}));
		} catch (Exception e) {
			//log.error("andLowerThan() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> orLowerThan(String field, Object value){
		try {
			criteria.add(new Where(Where.OR, DAOReflect.getName(clazz, field), Where.LT, new Object[]{value}));
		} catch (Exception e) {
			//log.error("orLowerThan() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> andEqualsLowerThan(String field, Object value){
		try {
			criteria.add(new Where(Where.AND, DAOReflect.getName(clazz, field), Where.ELT, new Object[]{value}));
		} catch (Exception e) {
			//log.error("andEqualsLowerThan() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> orEqualsLowerThan(String field, Object value){
		try {
			criteria.add(new Where(Where.OR, DAOReflect.getName(clazz, field), Where.ELT, new Object[]{value}));
		} catch (Exception e) {
			//log.error("orEqualsLowerThan() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> andLike(String field, String value){
		try {
			criteria.add(new Where(Where.AND, DAOReflect.getName(clazz, field), Where.LIKE, new Object[]{value}));
		} catch (Exception e) {
			//log.error("andLike() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> andRightLike(String field, String value){
		try {
			criteria.add(new Where(Where.AND, DAOReflect.getName(clazz, field), Where.RIGHT_LIKE, new Object[]{value+"%"}));
		} catch (Exception e) {
			//log.error("andLike() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> orLike(String field, String value){
		try {
			criteria.add(new Where(Where.OR, DAOReflect.getName(clazz, field), Where.LIKE, new Object[]{value}));
		} catch (Exception e) {
			//log.error("orLike() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> andNotLike(String field, Object value1){
		try {
			criteria.add(new Where(Where.AND, DAOReflect.getName(clazz, field), Where.NOT_LIKE, new Object[]{value1}));
		} catch (Exception e) {
			//log.error("andNotLike() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> andBeetwen(String field, Object value1, Object value2){
		try {
			criteria.add(new Where(Where.AND, DAOReflect.getName(clazz, field), Where.BETWEEN, new Object[]{value1, value2}));
		} catch (Exception e) {
			//log.error("andNotLike() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> andIn(String field, Object... values){
		try {
			criteria.add(new Where(Where.AND, DAOReflect.getName(clazz, field), Where.IN, values));
		} catch (Exception e) {
			//log.error("andIn() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> orIn(String field, Object... values){
		try {
			criteria.add(new Where(Where.OR, DAOReflect.getName(clazz, field), Where.IN, values));
		} catch (Exception e) {
			//log.error("orIn() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> andNotIn(String field, Object... values){
		try {
			criteria.add(new Where(Where.AND, DAOReflect.getName(clazz, field), Where.NOT_IN, values));
		} catch (Exception e) {
			//log.error("andNotIn() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> andIsNull(String field) {
		try {
			criteria.add(new Where(Where.AND, DAOReflect.getName(clazz, field), Where.IS_NULL));
		} catch (Exception e) {
			e.printStackTrace();
			//log.error("andNotIn() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> andIsNotNull(String field) {
		try {
			criteria.add(new Where(Where.AND, DAOReflect.getName(clazz, field), Where.IS_NOT_NULL));
		} catch (Exception e) {
			//log.error("andNotIn() :: No attribute for "+field, e);
			throw new RuntimeException("No attribute for "+field);
		}
		return this;
	}
	
	public Criteria<T> orderBy(String ...fields){
		this.orderBy = fields;
		return this;
	}
	
	public Criteria<T> groupBy(String... fields){
		this.groupBy = fields;
		return this;
	}
	
	public Criteria<T> limit(int rows){
		this.rows  = rows;
		return this;
	}
}


class Where {
	static final int AND = 0;
	static final int OR = 1;
	
	static final int EQ = 0;
	static final int GT = 1;
	static final int EGT = 2;
	static final int LT = 3;
	static final int ELT = 4;
	static final int LIKE = 5;
	static final int BETWEEN = 6;
	static final int IN = 7;
	static final int NEQ = 8;
	static final int NOT_LIKE = 9;
	static final int NOT_IN = 10;
	static final int IS_NULL = 11;
	static final int IS_NOT_NULL = 12;
	static final int RIGHT_LIKE = 13;
	
	public Where(int AND_OR, Field field, int operator){
		this.field = field;
		this.operator = operator;
	}
	
	public Where(int AND_OR, Field field, int operator, Object[] values){
		this.field = field;
		this.values = values;
		this.operator = operator;
	}
	
	Field field;
	Object[] values;
	int operator;
	int and_or;
	
	public String toString(){
		String result=(and_or==AND)?" and ":" or ";
		switch (operator) {
		case EQ:
			result += DAOReflect.getName(field)+" = ? ";
			break;
		case NEQ:
			result += DAOReflect.getName(field)+" <> ? ";
			break;
		case GT:
			result += DAOReflect.getName(field)+" > ? ";
			break;
		case EGT:
			result += DAOReflect.getName(field)+" >= ? ";
			break;
		case LT:
			result += DAOReflect.getName(field)+" < ? ";
			break;
		case ELT:
			result += DAOReflect.getName(field)+" <= ? ";
			break;
		case LIKE:
			result += DAOReflect.getName(field)+" like ? ";
			break;
		case RIGHT_LIKE:
			result += DAOReflect.getName(field)+" like ? ";
			break;
		case NOT_LIKE:
			result += DAOReflect.getName(field)+" not like ? ";
			break;
		case IS_NULL:
			result += DAOReflect.getName(field)+" is NULL ";
			break;
		case IS_NOT_NULL:
			result += DAOReflect.getName(field)+" is NOT NULL ";
			break;
		case BETWEEN:
			result += DAOReflect.getName(field)+" between ? and ? ";
			break;
		case IN:
			result += DAOReflect.getName(field)+" in ( ";
			for (int i = 0; i < values.length; i++) {
				if(i != 0)
					result += " , ";
				result += " ? ";
			}
			result += " ) ";
			break;
		case NOT_IN:
			result += DAOReflect.getName(field)+" not in ( ";
			for (int i = 0; i < values.length; i++) {
				if(i != 0)
					result += " , ";
				result += " ? ";
			}
			result += " ) ";
			break;
		default:
			break;
		}
		return result;
	}
}
