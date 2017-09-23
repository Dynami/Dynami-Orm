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

import java.util.List;

public class DAOPrimitives {
	
	@IEntity
	public static class String {
		@IField
		String value;
		
		public String getValue() {
			return value;
		}
		
		public void setValue(String value) {
			this.value = value;
		}
	}
	
	@IEntity
	public static class Double {
		@IField
		double value;
		
		public double getValue() {
			return value;
		}
		
		public void setValue(double value) {
			this.value = value;
		}

		public static double[] asArray(List<DAOPrimitives.Double> tmp) {
			return tmp.stream()
					.mapToDouble(DAOPrimitives.Double::getValue)
					.toArray();
		}
	}
	
	@IEntity
	public static class Float {
		@IField
		float value;
		
		public float getValue() {
			return value;
		}
		
		public void setValue(float value) {
			this.value = value;
		}
	}
	
	@IEntity
	public static class Long {
		@IField
		long value;
		
		public long getValue() {
			return value;
		}
		
		public void setValue(long value) {
			this.value = value;
		}
	}
	
	@IEntity
	public static class Short {
		@IField
		short value;
		
		public short getValue() {
			return value;
		}
		
		public void setValue(short value) {
			this.value = value;
		}
	}
	
	@IEntity
	public static class Int {
		@IField
		int value;
		
		public int getValue() {
			return value;
		}
		
		public void setValue(int value) {
			this.value = value;
		}
	}
	
	@IEntity
	public static class Boolean {
		@IField
		boolean value;
		
		public boolean getValue() {
			return value;
		}
		
		public void setValue(boolean value) {
			this.value = value;
		}
	}
}
