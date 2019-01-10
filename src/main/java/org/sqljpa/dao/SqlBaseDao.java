package org.sqljpa.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.sql.DataSource;

import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.sqljpa.mapper.FieldMapper;
import org.sqljpa.mapper.MapperFactory;
import org.sqljpa.mapper.ObjectTableInfo;
import org.sqljpa.mapper.SQLGenerator;
import org.sqljpa.util.SQLUtility;

public class SqlBaseDao<T> extends NamedParameterJdbcDaoSupport{
	@Autowired
	private DataSource dataSource;
	
	private Class<T> persistentClass;
	
	private SQLGenerator sqlGenerator;
	 
	private MapperFactory mapperFactory;
	
	public SqlBaseDao() {
		this.setSqlGenerator(SQLGenerator.getInstance()); 
	}
	
	
	public Class<T> getPersistentClass() {
		return persistentClass;
	}


	public void setPersistentClass(Class<T> persistentClass) {
		this.persistentClass = persistentClass;
	}


	public SQLGenerator getSqlGenerator() {
		return sqlGenerator;
	}


	public void setSqlGenerator(SQLGenerator sqlGenerator) {
		this.sqlGenerator = sqlGenerator;
	}


	public MapperFactory getMapperFactory() {
		return mapperFactory;
	}


	public void setMapperFactory(MapperFactory mapperFactory) {
		this.mapperFactory = mapperFactory;
	}


	protected Map<String, ?> getParam(Object param) {
		return getParam(param, false);
	}

	protected Map<String, ?> getParam(Object param, boolean ignoreConverter) {
		Map<String, Object> paramMap = null;
		if (param != null) {			
			ObjectTableInfo objectTable = mapperFactory.getObjectTableInfo(param.getClass());
			
			if (param instanceof Map) {
				paramMap = (Map<String, Object>) param;
			} else if(objectTable != null  ) {
				paramMap = new HashMap<String, Object>();
				Set<Entry<String, FieldMapper>> entrySet = objectTable.getColumnToPropertyMap().entrySet();
				for( Entry<String, FieldMapper> entry:entrySet) {
					FieldMapper field = entry.getValue();
					if(field.hasConverter()) {
						AttributeConverter converter = field.getConverter();						
						paramMap.put(field.getName(),
								converter.convertToDatabaseColumn(SQLUtility.getProperty(param, field.getName())));
					}else {
						paramMap.put(field.getName(), SQLUtility.getProperty(param, field.getName()));
					}
				}
			}else {				
				try {
						paramMap = (Map<String, Object>) PropertyUtils.describe(param);						
				} catch (Exception e) {
						throw new RuntimeException(e);
				}
			}
			
			if(!ignoreConverter && (objectTable == null) ){
				objectTable = mapperFactory.getObjectTableInfo(this.getPersistentClass());
				Set<Entry<String, FieldMapper>> entrySet = objectTable.getColumnToPropertyMap().entrySet();
				for( Entry<String, FieldMapper> entry:entrySet) {
					FieldMapper field = entry.getValue();
					if(field.hasConverter() 
							&& paramMap.containsKey(field.getName()) 
							&& paramMap.get(field.getName()).getClass().equals(field.getType())) {						
						paramMap.put(field.getName(),
								field.getConverter().convertToDatabaseColumn(paramMap.get(field.getName()))
								);
					}
				}
			}
				
		}
		return paramMap;
	}
	
	public void execute(String sql, Object... param) {
		this.getJdbcTemplate().update(sql, param);
	}

	public void execute(String sql, Object param) {
		this.getJdbcTemplate().update(sql, this.getParam(param, false));
	}

	public void execute(String sql, Map<String, ?> param) {
		this.getJdbcTemplate().update(sql, param);
	}

	protected void update(String sql, Object param) {
		this.getNamedParameterJdbcTemplate().update(sql, this.getParam(param, false));
	}
	
	public RowMapper getRowMapper(Class clazz){
		return getMapperFactory().getRowMapper(clazz);
	}

	public List<T> query(String sql, Object param) {
		RowMapper<T> rowMapper = getRowMapper(this.getPersistentClass());
		return this.getNamedParameterJdbcTemplate().query(sql, this.getParam(param, false), rowMapper);
	}	
	
	public List<T> list(Object param) {
		Map<String, Object> paramMap = (Map<String, Object>)this.getParam(param, false);
		ObjectTableInfo objectTable = getMapperFactory().getObjectTableInfo(this.getPersistentClass());	
		return list(this.getSqlGenerator().generateSelect(objectTable, paramMap),param);
	}
	
	public List<T> list(String sql,Object param) {
		Map<String, Object> paramMap = (Map<String, Object>)this.getParam(param, false);
		ObjectTableInfo objectTable = getMapperFactory().getObjectTableInfo(this.getPersistentClass());	
		RowMapper rowMapper = getMapperFactory().getRowMapper(this.getPersistentClass());
		return (List<T>)this.getNamedParameterJdbcTemplate().query(sql, paramMap, rowMapper);
	}
	
	public List<T> listHierarchy(Object param) {
		Map<String, Object> paramMap = (Map<String, Object>)this.getParam(param, false);
		ObjectTableInfo objectTable = getMapperFactory().getObjectTableInfo(this.getPersistentClass());	
		return listHierarchy(this.getSqlGenerator().generateSelect(objectTable, paramMap),param);
	}
	
	public List<T> listHierarchy(String sql,Object param) {
		Map<String, Object> paramMap = (Map<String, Object>)this.getParam(param, false);
		ObjectTableInfo objectTable = getMapperFactory().getObjectTableInfo(this.getPersistentClass());	
		ResultSetExtractor extractor = getMapperFactory().getExtractor(this.getPersistentClass());
		return (List<T>)this.getNamedParameterJdbcTemplate().query(sql, paramMap, extractor);
	}
	

	@Override
	protected void checkDaoConfig() throws IllegalArgumentException {
		super.setDataSource(this.dataSource);
		super.checkDaoConfig();
	}
}
