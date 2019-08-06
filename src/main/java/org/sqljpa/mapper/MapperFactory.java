package org.sqljpa.mapper;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Id;
import javax.persistence.Table;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.sqljpa.annotation.Audited;
import org.sqljpa.annotation.CollectionOfEntity;
import org.sqljpa.annotation.MapOfEntity;
import org.sqljpa.annotation.SQLSequence;
import org.sqljpa.annotation.VersionColumn;
import org.sqljpa.util.SQLUtility;


public abstract class MapperFactory {

	private static MapperFactory instance;

	private SQLGenerator sqlGenerator;

	private Map<String, ObjectTableInfo> objectTableInfoMap = new HashMap<String, ObjectTableInfo>();

	public static MapperFactory getInstance() {
		if (instance == null) {
			instance = new MapperFactory() {
			};
			instance.sqlGenerator = SQLGenerator.getInstance();
		}
		return instance;
	}

	public void setSqlGenerator(SQLGenerator sqlGenerator) {
		this.sqlGenerator = sqlGenerator;
	}

	/**
	 * @param clazz
	 * @return
	 */
	public ObjectTableInfo generateObjectTableInfo(Class clazz) {
		ObjectTableInfo objectTableInfo = getInstance().objectTableInfoMap.get(clazz.getName());
		if (objectTableInfo == null) {
			getInstance().introspect(clazz);
			return getInstance().objectTableInfoMap.get(clazz.getName());
		}
		return objectTableInfo;
	}

	/**
	 * @param clazz
	 * @return
	 */
	public ObjectTableInfo getObjectTableInfo(Class clazz) {
		return getInstance().objectTableInfoMap.get(clazz.getName());
	}

	/**
	 * @param clazz
	 */
	private ObjectTableInfo introspect(Class clazz) { //TODO Optimistic locking tag
		String tableName = null;
		ObjectTableInfo objectTableInfo = new ObjectTableInfo();
		Annotation[] ana = clazz.getAnnotations();
		for (Annotation an : ana) {
			if (an instanceof Table) {
				tableName = ((Table) an).name();
			}
		}
		objectTableInfo.setEntityClass(clazz);
		objectTableInfo.setTableName(tableName);
		Map<String, FieldMapper> columnToProperty = new HashMap<String, FieldMapper>();
		Field[] fields = clazz.getDeclaredFields();
		for (Field f : fields) {
			FieldMapper field = new FieldMapper();
			field.setName(f.getName());
			field.setColumn(f.getName());
			field.setType(f.getType());
			ana = f.getAnnotations();
			for (Annotation an : ana) {
				if (an instanceof Id) {
					objectTableInfo.setIdType(f.getType());
					objectTableInfo.setIdName(f.getName());
				}
				if ((an instanceof Column) ||(an instanceof VersionColumn)) {
					field.setColumn(((Column) an).name());
					if (!((Column) an).updatable()) {
						field.setReadonly(true);
					}
					if (an instanceof VersionColumn) {
						field.setDbScriptUpdateValue(((VersionColumn) an).getValueFrom());
					}
					try {
						PropertyDescriptor descriptor = new PropertyDescriptor(f.getName(),
								objectTableInfo.getEntityClass(), "get" + SQLUtility.capitalize(f.getName()),
								"set" + SQLUtility.capitalize(f.getName()));
						field.setSetter(descriptor.getWriteMethod());
						field.setGetter(descriptor.getReadMethod());
					} catch (IntrospectionException e) {
						throw new RuntimeException(e);
					}
					columnToProperty.put(f.getName(), field);
				}
				if (an instanceof Convert) {
					try {
						Class converter = ((Convert) an).converter();
						field.setConverter((AttributeConverter) converter.newInstance());
						Method[] ma = converter.getMethods();
						for(Method m:ma) {
							if(m.getName().equals("convertToDatabaseColumn")) {								
								field.setConverterDatabaseType(m.getReturnType());
							}
                            if(m.getName().equals("convertToEntityAttribute")) {
                            	field.setConverterEntityType(m.getReturnType());
							}
						}
					} catch (InstantiationException e) {
						throw new RuntimeException(e);
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					}
				}
				if (an instanceof SQLSequence) {
					objectTableInfo.setSequenceGenerator(((SQLSequence) an).sql());
					objectTableInfo.setSequenceFunction(((SQLSequence) an).function());
				}
				if (an instanceof Audited) {
					Class auditEntity = ((Audited) an).auditEntity();
					objectTableInfo.setAuditInfo(this.generateObjectTableInfo(auditEntity));
					objectTableInfo.setHistoryTable(((Audited) an).historyTable());
					objectTableInfo.setAuditId(f.getName());
				}
				
				
				if (an instanceof CollectionOfEntity) {
					CollectionFieldMapper colField = new CollectionFieldMapper();
					colField.setType(f.getType());
					try {
						PropertyDescriptor descriptor = new PropertyDescriptor(f.getName(),
								objectTableInfo.getEntityClass(), "get" + SQLUtility.capitalize(f.getName()),
								"set" + SQLUtility.capitalize(f.getName()));
						colField.setSetter(descriptor.getWriteMethod());
						colField.setGetter(descriptor.getReadMethod());
					} catch (IntrospectionException e) {
						throw new RuntimeException(e);
					}
					
					Class entity =  ((CollectionOfEntity) an).entity();
					colField.setItemType(entity);
					colField.setColumnPrefix(((CollectionOfEntity) an).prefix());
					colField.setColumn(((CollectionOfEntity) an).column());	
					colField.setColumnKey(((CollectionOfEntity) an).columnKey());
					colField.setCollectionType(((CollectionOfEntity) an).collectionType());
					ObjectTableInfo info = null;
					if(Collection.class.isAssignableFrom(entity)) {						
						 info = introspect(((CollectionOfEntity) an).paramType());
						 if(info == null) {
							 CollectionFieldMapper nestedNativeType = new CollectionFieldMapper();
							 nestedNativeType.setType(entity);
							 nestedNativeType.setItemType(((CollectionOfEntity) an).paramType());
							 nestedNativeType.setColumn(((CollectionOfEntity) an).column());
							 nestedNativeType.setColumnPrefix(((CollectionOfEntity) an).prefix());
							 colField.setNestedNativeType(nestedNativeType);
						 }
						 
					}else {
						 info = introspect(entity);
					}
					colField.setCollectionInfo(info);
					objectTableInfo.setCollectionField(colField);
				}
				if (an instanceof MapOfEntity) {
					CollectionFieldMapper mapField = new CollectionFieldMapper();
					mapField.setType(f.getType());
					try {
						PropertyDescriptor descriptor = new PropertyDescriptor(f.getName(),
								objectTableInfo.getEntityClass(), "get" + SQLUtility.capitalize(f.getName()),
								"set" + SQLUtility.capitalize(f.getName()));
						mapField.setSetter(descriptor.getWriteMethod());
						mapField.setGetter(descriptor.getReadMethod());
					} catch (IntrospectionException e) {
						throw new RuntimeException(e);
					}
					Class entity =  ((MapOfEntity) an).entity();
					mapField.setItemType(entity);
					mapField.setColumnPrefix(((MapOfEntity) an).prefix());
					mapField.setColumn(((MapOfEntity) an).column());
					mapField.setColumnKey(((MapOfEntity) an).columnKey());
					mapField.setCollectionType(((MapOfEntity) an).collectionType());
					ObjectTableInfo info = null;
					if(Collection.class.isAssignableFrom(entity)) {						
						 info = introspect(((MapOfEntity) an).paramType());
						 if(info == null) {
							 CollectionFieldMapper nestedNativeType = new CollectionFieldMapper();
							 nestedNativeType.setType(entity);
							 nestedNativeType.setItemType(((MapOfEntity) an).paramType());
							 nestedNativeType.setColumn(((MapOfEntity) an).column());
							 nestedNativeType.setColumnPrefix(((MapOfEntity) an).prefix());
							 mapField.setNestedNativeType(nestedNativeType);
						 }
						 
					}else {
						 info = introspect(entity);
					}
					mapField.setCollectionInfo(info);
					objectTableInfo.setMapField(mapField);
				}
			}
		}
		if (columnToProperty.isEmpty()) {
			return null;
		}
		objectTableInfo.setColumnToPropertyMap(columnToProperty);
		if (tableName != null) {
			objectTableInfo.setInsertSql(SQLGenerator.getInstance().generateInsert(objectTableInfo, false));
			objectTableInfo.setInsertSqlBatch(SQLGenerator.getInstance().generateInsert(objectTableInfo, true));
			objectTableInfo.setRemoveByIdSql(SQLGenerator.getInstance().generateRemoveById(objectTableInfo));
			objectTableInfo.setUpdateSql(SQLGenerator.getInstance().generateUpdate(objectTableInfo));
		}
		objectTableInfoMap.put(clazz.getName(), objectTableInfo);
		return objectTableInfo;
	}

	/**
	 * @param clazz
	 * @return
	 */
	public RowMapper getRowMapper(Class clazz) {
		RowMapper rm = new RowMapper() {
			private Class mapperClazz = clazz;
			private Map<String, FieldMapper> mapperColumnToProperty = getObjectTableInfo(clazz)
					.getColumnToPropertyMap();

			@Override
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				try {
					Object obj = mapperClazz.newInstance();
					Set<Entry<String, FieldMapper>> entrySet = mapperColumnToProperty.entrySet();
					for (Entry<String, FieldMapper> entry : entrySet) {
						try {
							FieldMapper f = entry.getValue();
							// String col = f.getColumn(); // System.out.println("Field type:
							// "+f.getType().getName());
							setObject(obj, rs, f.getColumn(), f);
						} catch (SQLException e) {
							// e.printStackTrace();
							// Supress exception if the column was not found;
						} catch (IllegalArgumentException | InvocationTargetException e) {
							throw new RuntimeException(e);
						}
					}
					return obj;
				} catch (InstantiationException e) {
					throw new SQLException(e.getMessage());
				} catch (IllegalAccessException e) {
					throw new SQLException(e.getMessage());
				}
			}
		};
		return rm;
	}

	public ResultSetExtractor getExtractor(Class clazz) {
		ResultSetExtractor rse = new ResultSetExtractor() {
			private Class mapperClazz = clazz;
			private ObjectTableInfo objectTableInfo = getObjectTableInfo(clazz);
			private CollectionFieldMapper collectionField = objectTableInfo.getCollectionField();
			private CollectionFieldMapper mapField = objectTableInfo.getMapField();
			private String mapColumnKey = null;
			private String listColumnKey = null;
			private Collection subList = null;
			private Map subjMap = null;

			private FieldMapper getCollectionItemField() {
				FieldMapper collectionItemField = null;	
				if(collectionField != null && collectionField.getCollectionInfo() == null) {
					collectionItemField = new FieldMapper();
					collectionItemField.setColumn(collectionField.getColumn());
					collectionItemField.setConverter(collectionField.getConverter());
					collectionItemField.setType(collectionField.getItemType());	
					if(Collection.class.isAssignableFrom(collectionField.getItemType())) {
						collectionItemField.setType(collectionField.getNestedNativeType().getItemType());
					}
				}
				return collectionItemField;
			}
			
			private FieldMapper getMapItemField() {
				FieldMapper mapItemField = null;	
				if(mapField != null && mapField.getCollectionInfo() == null) {
					mapItemField = new FieldMapper();
					mapItemField.setColumn(mapField.getColumn());
					mapItemField.setConverter(mapField.getConverter());
					mapItemField.setType(mapField.getItemType());
					if(Collection.class.isAssignableFrom(mapField.getItemType())) {
						mapItemField.setType(mapField.getNestedNativeType().getItemType());
					}
				}
				return mapItemField;
			}
			
			private void populateSublist(ResultSet rs,Collection subList,FieldMapper collectionItemField) 
					throws InstantiationException, IllegalAccessException, IllegalArgumentException, 
					InvocationTargetException, SQLException {
				if (collectionField != null) {
					if(collectionField.getCollectionInfo() != null) {
						Object subObj = collectionField.getCollectionInfo().getEntityClass().newInstance();
						populateObject(subObj, rs, collectionField.getCollectionInfo().getColumnToPropertyMap(),
								collectionField.getColumnPrefix());
						subList.add(subObj);
					}else {
						subList.add(getObject(rs, collectionItemField.getColumn(), collectionItemField));
					}
				}
			}
			
           private Collection populateSublistObj(ResultSet rs,Collection subList,FieldMapper collectionItemField,Collection colList) 
        		   throws InstantiationException, IllegalAccessException, IllegalArgumentException, 
					InvocationTargetException, SQLException{ 
        	   if(collectionField == null) {
        		   return null;
        	   }
        	   Collection col = colList;
        	   if( Collection.class.isAssignableFrom(collectionField.getItemType())) {        		   
        		   String newListColumnKey =  (String)getValue(rs, collectionField.getColumnKey(), String.class);
        		   if(newListColumnKey.equals(listColumnKey)) {        			  
						if(collectionField.getCollectionInfo() != null) {
							Object subObj = collectionField.getCollectionInfo().getEntityClass().newInstance();
							populateObject(subObj, rs, collectionField.getCollectionInfo().getColumnToPropertyMap(),
									collectionField.getColumnPrefix());
							col.add(subObj);							
						}else {										
							col.add(getObject(rs, collectionItemField.getColumn(), collectionItemField));
						}
        		   }else {
        			   listColumnKey = newListColumnKey;
						col = (Collection)collectionField.getItemType().newInstance();
						if(collectionField.getCollectionInfo() != null) {
							Object subObj = collectionField.getCollectionInfo().getEntityClass().newInstance();
							populateObject(subObj, rs, collectionField.getCollectionInfo().getColumnToPropertyMap(),
									collectionField.getColumnPrefix());
							col.add(subObj);							
						}else {										
							col.add(getObject(rs, collectionItemField.getColumn(), collectionItemField));
						}
						subList.add(col);						
        		   }
        	   }else {
        		   this.populateSublist(rs, subList, collectionItemField);
        	   }
        	   return col;
			}
			
			private void populateSubMap(ResultSet rs,Map subjMap,FieldMapper mapItemField) 
					throws InstantiationException, IllegalAccessException, IllegalArgumentException, 
					InvocationTargetException, SQLException {
				if(mapField == null) {
					return;
				}
				if (mapField != null) {
					if(mapField.getCollectionInfo() != null) {
						Object subObj = mapField.getCollectionInfo().getEntityClass().newInstance();
						populateObject(subObj, rs, mapField.getCollectionInfo().getColumnToPropertyMap(),
								mapField.getColumnPrefix());
						subjMap.put(getValue(rs,mapField.getColumnKey(),String.class), 
								subObj);
					}else {
						subjMap.put(getValue(rs,mapField.getColumnKey(),String.class), 
								getObject(rs, mapItemField.getColumn(), mapItemField));						
					}
				}
			}
			
			
			
			private String populateSubMapObj(ResultSet rs,Map subjMap,FieldMapper mapItemField,String columnKey) 
					throws InstantiationException, IllegalAccessException, IllegalArgumentException, 
					InvocationTargetException, SQLException{
				if(mapItemField == null) {
					return null;
				}
				String mapColumnKey = columnKey;
				if(Collection.class.isAssignableFrom(mapField.getItemType())) {					
					String newMapColumnKey =  (String)getValue(rs, mapField.getColumnKey(), String.class);
					if(newMapColumnKey.equals(mapColumnKey)) {
						Collection col = (Collection)subjMap.get(mapColumnKey);
						if(mapField.getCollectionInfo() != null) {
							Object subObj = mapField.getCollectionInfo().getEntityClass().newInstance();
							populateObject(subObj, rs, mapField.getCollectionInfo().getColumnToPropertyMap(),
									mapField.getColumnPrefix());
							col.add(subObj);
							
						}else {										
							col.add(getObject(rs, mapItemField.getColumn(), mapItemField));
						}
					}else {
						mapColumnKey = newMapColumnKey;
						Collection col = (Collection)mapField.getItemType().newInstance();
						if(mapField.getCollectionInfo() != null) {
							Object subObj = mapField.getCollectionInfo().getEntityClass().newInstance();
							populateObject(subObj, rs, mapField.getCollectionInfo().getColumnToPropertyMap(),
									mapField.getColumnPrefix());
							col.add(subObj);
							
						}else {										
							col.add(getObject(rs, mapItemField.getColumn(), mapItemField));
						}
						subjMap.put(mapColumnKey, 
								col);
					}
				}else {
					this.populateSubMap(rs, subjMap, mapItemField);
				}
				return mapColumnKey;
			}

			@Override
			public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
				rs.beforeFirst();
				Object previd = null;
				Object obj = null;
				List objList = new ArrayList();
				
				FieldMapper collectionItemField =  this.getCollectionItemField();
				FieldMapper mapItemField = this.getMapItemField();
				try {
					
					Collection subListcolList = null;
					while (rs.next()) {
						FieldMapper idField = objectTableInfo.getColumnToPropertyMap().get(objectTableInfo.getIdName());
						Object id = null;
						if(idField != null ) {
							id = getObject(rs, idField.getColumn(), idField);
						}
						
						if (previd != null && previd.equals(id)) {
							//this.populateSublist(rs, subList, collectionItemField);
							subListcolList = this.populateSublistObj(rs, subList, collectionItemField, subListcolList);
							mapColumnKey = this.populateSubMapObj(rs, subjMap, mapItemField, mapColumnKey);
						} else {
							obj = mapperClazz.newInstance();
							objList.add(obj);
							if (collectionField != null) {
								subList = (Collection)collectionField.getCollectionType().newInstance();
								collectionField.getSetter().invoke(obj, subList);
								subListcolList = this.populateSublistObj(rs, subList, collectionItemField, subListcolList);
								//this.populateSublist(rs, subList, collectionItemField);
							}
							if(mapField != null) {
								subjMap = (Map)mapField.getCollectionType().newInstance();
								mapField.getSetter().invoke(obj, subjMap);
								mapColumnKey = this.populateSubMapObj(rs, subjMap, mapItemField, null);
							}
							
							populateObject(obj, rs, objectTableInfo.getColumnToPropertyMap());
						}
						previd = id;
					}
				} catch (InstantiationException e) {
					throw new SQLException(e.getMessage());
				} catch (IllegalAccessException e) {
					throw new SQLException(e.getMessage());
				} catch (IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
				return objList;
			}

		};
		return rse;
	}

	private static void populateObject(Object obj, ResultSet rs, Map<String, FieldMapper> mapperColumnToProperty)
			throws IllegalArgumentException, InvocationTargetException, IllegalAccessException {
		populateObject(obj, rs, mapperColumnToProperty, "");
	}

	private static Object populateObject(Object obj, ResultSet rs, Map<String, FieldMapper> mapperColumnToProperty,
			String prefix) throws IllegalArgumentException, InvocationTargetException, IllegalAccessException {
		Set<Entry<String, FieldMapper>> entrySet = mapperColumnToProperty.entrySet();
		for (Entry<String, FieldMapper> entry : entrySet) {
			try {
				FieldMapper f = entry.getValue();
				if(prefix == null) {
					setObject(obj, rs,f.getColumn(), f);
				}else {
					setObject(obj, rs, prefix + f.getColumn(), f);
				}				
			} catch (SQLException e) {
				// e.printStackTrace();
				// Supress exception if the column was not found;
			}
		}
		return obj;
	}

	private static void setObject(Object obj, ResultSet rs, String col, FieldMapper f)
			throws SQLException, IllegalArgumentException, InvocationTargetException, IllegalAccessException {
		f.getSetter().invoke(obj, getObject(rs, col, f));
	}

	private static Object getObject(ResultSet rs, String col, FieldMapper f)
			throws SQLException, IllegalArgumentException, InvocationTargetException, IllegalAccessException {
		if (f.hasConverter()) {
			return f.getConverter().convertToEntityAttribute(getValue(rs, col, f.getConverterDatabaseType()));			
		} else {
			return getValue(rs, col, f.getType());
		}
	}
	
	private static Object getValue(ResultSet rs, String col, Class type) 
			throws SQLException, IllegalArgumentException, InvocationTargetException, IllegalAccessException{
		if (type.equals(int.class)) {
			return rs.getInt(col);
		} else if (type.equals(long.class)) {
			return rs.getLong(col);
		} else if (type.equals(float.class)) {
			return rs.getFloat(col);
		} else if (type.equals(double.class)) {
			return rs.getDouble(col);
		} else if (type.equals(byte.class)) {
			return rs.getByte(col);
		} else if (type.equals(short.class)) {
			return rs.getShort(col);
		} else if (type.equals(char.class)) {
			return rs.getString(col);
		} else if (type.equals(boolean.class)) {
			return rs.getBoolean(col);
		} else if (type.equals(Integer.class)) {
			return rs.getInt(col);
		} else if (type.equals(Long.class)) {
			return rs.getLong(col);
		} else if (type.equals(Float.class)) {
			return rs.getFloat(col);
		} else if (type.equals(Double.class)) {
			return rs.getDouble(col);
		} else if (type.equals(Byte.class)) {
			return rs.getByte(col);
		} else if (type.equals(Short.class)) {
			return rs.getShort(col);
		} else if (type.equals(String.class)) {
			return rs.getString(col);
		} else if (type.equals(Boolean.class)) {
			return rs.getBoolean(col);
		} else if (type.equals(Date.class)) {			
			return rs.getTimestamp(col);
		} else if (type.equals(Timestamp.class)) {			
			return rs.getTimestamp(col);	
		} else if (type.equals(BigDecimal.class)) {
			return rs.getBigDecimal(col);
		} else if (type.equals(BigInteger.class)) {
			return BigInteger.valueOf(rs.getLong(col));
		} else {
			return rs.getObject(col);
		}
	}
}
