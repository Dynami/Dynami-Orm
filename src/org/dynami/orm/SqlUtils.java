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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SqlUtils {
	static PreparedStatement sqlSelect(Connection con, Object entity) throws Exception {
		StringBuilder buffer = new StringBuilder("select * ");
		buffer.append(" from ");
		buffer.append(DAOReflect.getTableName(entity));
		buffer.append(" where 1 = 1 ");
		Field[] pk = DAOReflect.pk(entity);
		for (int i = 0; i < pk.length; i++) {
			buffer.append(" and ");
			buffer.append(DAOReflect.getName(pk[i]));
			buffer.append(" = ? ");
		}
		
		PreparedStatement pstmt = con.prepareStatement(buffer.toString());
		for (int i = 0; i < pk.length; i++) {
			pstmt.setObject(i+1, DAOReflect.get(entity, pk[i]));
		}
		return pstmt;
	}
	
	static PreparedStatement sqlUpdate(Connection con, Object entity, String[] exclude) throws Exception {
		StringBuilder buffer = new StringBuilder("update ");
		buffer.append(DAOReflect.getTableName(entity));
		buffer.append(" set ");
		Field[] fields = DAOReflect.fields(entity, true); //entity.getClass().getDeclaredFields();
		String fieldName = null;
		boolean comma = false;
		for (int i = 0; i < fields.length; i++) {
			fieldName = DAOReflect.getName(fields[i]);
			if(!contains(exclude, fieldName)){
				if(comma)
					buffer.append(", ");
				buffer.append(fieldName);
				buffer.append(" = ? ");
				comma = true;
			}
		}
		buffer.append(" where 1 = 1 ");
		Field[] pk = DAOReflect.pk(entity);
		for (int i = 0; i < pk.length; i++) {
			buffer.append(" and ");
			buffer.append(DAOReflect.getName(pk[i]));
			buffer.append(" = ? ");
		}
		
		PreparedStatement pstmt = con.prepareStatement(buffer.toString());
		int idx = 1;
		Object obj;
		for (int i = 0; i < fields.length; i++) {
			if(!contains(exclude, fields[i].getName())){
				obj = DAOReflect.get(entity, fields[i]);
				//System.out.println(obj);
				if(obj != null){
					if(obj.getClass().equals(Boolean.class)){
						pstmt.setObject(idx++, ((Boolean)obj)?1:0);
					} else {
						pstmt.setObject(idx++, obj);
					}
				} else {
					if(fields[i].getType().equals(String.class)){
						try{
							pstmt.setNull(idx++, Types.CHAR);
						}catch(Exception e){
							pstmt.setNull(idx, Types.VARCHAR);
						}
					} else if(fields[i].getType().equals(java.util.Date.class)){
						pstmt.setNull(idx++, Types.DATE);
					} else if(fields[i].getType().equals(double.class)){
						pstmt.setNull(idx++, Types.DOUBLE);
					} else if(fields[i].getType().equals(boolean.class)){
						pstmt.setNull(idx++, Types.BOOLEAN);
					} else {
						pstmt.setNull(idx++, Types.NUMERIC);
					}
				}				
			}
		}
		//System.out.println("------");
		for (int i = 0; i < pk.length; i++) {
			pstmt.setObject(idx++, DAOReflect.get(entity, pk[i]));
		}
		return pstmt;
	}
	
	static PreparedStatement sqlUpdate(Connection con, Object entity) throws Exception {
		return sqlUpdate(con, entity, new String[0]);
	}
	
	static PreparedStatement sqlDelete(Connection con, Object entity) throws Exception {
		StringBuilder buffer = new StringBuilder("delete from ");
		buffer.append(DAOReflect.getTableName(entity));
		buffer.append(" where 1 = 1 ");
		Field[] pk = DAOReflect.pk(entity);
		for (int i = 0; i < pk.length; i++) {
			buffer.append(" and ");
			buffer.append(DAOReflect.getName(pk[i]));
			buffer.append(" = ? ");
		}
		PreparedStatement pstmt = con.prepareStatement(buffer.toString());
		int idx = 1;
		for (int i = 0; i < pk.length; i++) {
			pstmt.setObject(idx++, DAOReflect.get(entity, pk[i]));
		}
		return pstmt;
	}
	
	static PreparedStatement sqlInsert(Connection con, Object entity) throws Exception {
		StringBuilder ins = new StringBuilder("insert into ");
		StringBuilder values = new StringBuilder(" values ( ");
		ins.append(DAOReflect.getTableName(entity));
		
		ins.append(" ( ");
		Field[] fields = DAOReflect.fields(entity, true); //entity.getClass().getDeclaredFields();
		boolean commaSep = false;
		List<Object> insertData = new ArrayList<Object>();
		for (int i = 0; i < fields.length; i++) {
			if(!DAOReflect.isSerial(fields[i])){
				if(commaSep){
					ins.append(", ");
					values.append(", ");
				}
				
				ins.append(DAOReflect.getName(fields[i]));
				values.append(" ? ");
				commaSep = true;
				insertData.add(DAOReflect.get(entity, fields[i]));
			}
		}
		ins.append(" ) ");
		values.append(" ) ");
		//System.out.println(ins.toString()+values.toString());
		PreparedStatement pstmt = con.prepareStatement(ins.toString()+values.toString());
		int idx = 1; 
		for (int i = 0; i < insertData.size(); i++) {
			if(insertData.get(i) instanceof Boolean){
				pstmt.setObject(idx++, ((Boolean)insertData.get(i))?1:0);
			} else {
				pstmt.setObject(idx++, insertData.get(i));
			}
		}
		return pstmt;
	}
	
	static <T> PreparedStatement sqlDeleteByCriteria(Connection con, Criteria<T> criteria) throws Exception {
		Object entity = criteria.getIEntity();
		StringBuilder buffer = new StringBuilder("delete from ");
		buffer.append(DAOReflect.getTableName(entity));
		buffer.append(" where 1 = 1 ");
		
		List<Where> deleteCriteria =  criteria.getCriteria();
		List<Object> deleteData = new ArrayList<Object>();
		Where where = null;
		for (Iterator<Where> iterator = deleteCriteria.iterator(); iterator.hasNext();) {
			where = iterator.next();
			buffer.append(where.toString());
			if(where.operator != Where.IS_NULL && where.operator != Where.IS_NOT_NULL){
				Collections.addAll(deleteData, where.values);
			}
		}
		PreparedStatement pstmt = con.prepareStatement(buffer.toString());
		int idx = 1;
		Object tmp;
		for(Iterator<Object> iterator = deleteData.iterator(); iterator.hasNext();){
			tmp = iterator.next();
			pstmt.setObject(idx++, tmp);
		}
		
		return pstmt;
	}
	
	static <T> PreparedStatement sqlSelectByCriteria(Connection con, Criteria<T> criteria) throws Exception {
		StringBuilder buffer = new StringBuilder("select ");
		
		if(criteria.distinct.length == 0){
			buffer.append(" * ");
		} else {
			buffer.append(" distinct ");
			for(int i = 0; i < criteria.distinct.length; i++){
				if(i>0){
					buffer.append(", ");
				}
				buffer.append(criteria.distinct[i]);
				
			}
		}
		Object entity = criteria.getIEntity();
		
		buffer.append(" from ");
		buffer.append(DAOReflect.getTableName(entity));
		buffer.append(" where 1 = 1 ");
		
		List<Where> searchCriteria =  criteria.getCriteria();
		List<Object> searchData = new ArrayList<Object>();
		Where where = null;
		for (Iterator<Where> iterator = searchCriteria.iterator(); iterator.hasNext();) {
			where = iterator.next();
			buffer.append(where.toString());
			if(where.operator != Where.IS_NULL && where.operator != Where.IS_NOT_NULL){
				Collections.addAll(searchData, where.values);
			}
		}
		if(criteria.groupBy != null && criteria.groupBy.length > 0){
			buffer.append(" group by ");
			for (int i = 0; i < criteria.groupBy.length ; i++) {
				if(i != 0) buffer.append(", ");
				
				buffer.append(criteria.groupBy[i]);
			}
		}
		if(criteria.orderBy != null && criteria.orderBy.length > 0){
			buffer.append(" order by ");
			for (int i = 0; i < criteria.orderBy.length ; i++) {
				if(i != 0) buffer.append(", ");
				
				buffer.append(criteria.orderBy[i]);
			}
		}
		if(criteria.rows > 0){
			buffer.append(" limit ");
			buffer.append(criteria.rows);
		}
//		System.out.println("DAO.sqlSelectByCriteria() "+buffer.toString());
		PreparedStatement pstmt = con.prepareStatement(buffer.toString());
		int idx = 1;
		Object tmp;
		for(Iterator<Object> iterator = searchData.iterator(); iterator.hasNext();){
			tmp = iterator.next();
			pstmt.setObject(idx++, tmp);
		}
		
		return pstmt;
	}
	
	
	private static boolean contains(String[] input, String match){
		for (int i = 0; i < input.length; i++) {
			if(input[i].equals(match)){
				return true;
			}
		}
		return false;
	}
	
	static void closeAll(PreparedStatement pstmt, ResultSet res) {
		try {
			pstmt.close();
		} catch (Exception e) {}
		try {
			res.close();
		} catch (Exception e) {}
	}
	
	static void closeAll(PreparedStatement pstmt) {
		try {
			pstmt.close();
		} catch (Exception e) {}
		
	}
	
	static void closeAll(Statement pstmt) {
		try {
			pstmt.close();
		} catch (Exception e) {}
	}
}
