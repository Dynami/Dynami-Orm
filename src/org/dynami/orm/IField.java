/*
 * Copyright 2013 Alessandro Atria - a.atria@gmail.com
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface IField {
	
	/**
	 * Indicates field as Primary key
	 * @return
	 */
	boolean pk() default false;
	/**
	 * Is treated as a primary key, but is not constrained (generates index on field)
	 * @return
	 */
	boolean vpk() default false;
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
