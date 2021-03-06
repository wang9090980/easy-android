/*
 * Copyright 2012 Evgeny Dolganov (evgenij.dolganov@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package easydroid.util;

import java.lang.reflect.Field;

public class ReflectionsUtil {
	
	
	@SuppressWarnings("unchecked")
	public static <T> T getField(Object obj, String fieldName) throws Exception {
		Field field = null;
		Class<?> curType = obj.getClass();
		while(curType != null){
			try {
				field = curType.getDeclaredField(fieldName);
				curType = null;
			}catch (NoSuchFieldException e) {
				curType = curType.getSuperclass();
			}
		}
		if(field == null){
			throw new NoSuchFieldException(fieldName);
		}
		
		field.setAccessible(true);
		return (T) field.get(obj);
	}

}
