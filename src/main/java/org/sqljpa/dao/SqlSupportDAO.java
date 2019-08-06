package org.sqljpa.dao;

import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.sql.DataSource;

import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.sqljpa.mapper.MapperFactory;
import org.sqljpa.mapper.FieldMapper;
import org.sqljpa.mapper.ObjectTableInfo;
import org.sqljpa.mapper.SQLGenerator;
import org.sqljpa.util.ParamMap;
import org.sqljpa.util.SQLUtility;

public class SqlSupportDAO<T> extends SqlBaseDao<T> {
	
	private AuditStrategy auditStrategy;
	
	public SqlSupportDAO() {
		this.setPersistentClass( (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
				.getActualTypeArguments()[0]);
		this.setMapperFactory(MapperFactory.getInstance());
		this.getMapperFactory().generateObjectTableInfo(this.getPersistentClass());
	}

	protected Map<String, ?> getParam(Object param) {
		return getParam(param, false);
	}
	
	public T getObject(Object id) {
		ObjectTableInfo objectTable = getMapperFactory().getObjectTableInfo(this.getPersistentClass());
		Object[] ida = {id};		
		return (T)this.getJdbcTemplate().queryForObject(
				this.getSqlGenerator().generateSingleSelect(objectTable), ida, getRowMapper(this.getPersistentClass()));		
	}
	
	public T getById(Object id) {
		return getObject(id);		
	}

	public T insert(T t, boolean generateId) {
		ObjectTableInfo objectTable = getMapperFactory().getObjectTableInfo(t.getClass());
		if(generateId) {
			SQLUtility.setProperty(t, objectTable.getIdName(),
					this.getJdbcTemplate().queryForObject(objectTable.getSequenceGenerator(), objectTable.getIdType()));
		}
		Object auditId = this.logAudit(new ParamMap("action","Insert")) ;
		if(auditId != null) {
			SQLUtility.setProperty(t, objectTable.getAuditId(),auditId);
		}
		this.update(objectTable.getInsertSql(), t);
		return t;
	}

	public int batchInsert(List<T> list) {
		if ((list != null) && (!list.isEmpty())) {						
			T first = list.get(0);
			ObjectTableInfo objectTable = getMapperFactory().getObjectTableInfo(first.getClass());
			Object auditId = auditStrategy.logAudit(objectTable, new ParamMap("action","Batch Insert")) ;
			Map<String, ?>[] paramAr = new Map[list.size()];
			int i = 0;
			for (T t : list) {
				if(auditId != null) {
					SQLUtility.setProperty(t, objectTable.getAuditId(),auditId);
				}
				
				paramAr[i++] = getParam(t, false);
			}
			
			return this.getNamedParameterJdbcTemplate()
					.batchUpdate(objectTable.getInsertSqlBatch(), paramAr).length;
		}
		return 0;
	}
		
	protected Object logAudit(Map<String,Object> param) {
		ObjectTableInfo objectTable = getMapperFactory().getObjectTableInfo(this.getPersistentClass());		
		return auditStrategy.logAudit(objectTable, param);
	}
	protected int insertHistory(Map<String,Object> param) {
		ObjectTableInfo objectTable = getMapperFactory().getObjectTableInfo(this.getPersistentClass());
		return auditStrategy.logHistory(objectTable, param);		
	}
	
	public int update(T t) {
		ObjectTableInfo objectTable = getMapperFactory().getObjectTableInfo(t.getClass());
		return this.getNamedParameterJdbcTemplate().update(objectTable.getUpdateSql(),this.getParam(t));
	}
	
	public int update(Map<String,Object> param) { //TODO Optimistic locking
		ObjectTableInfo objectTable = getMapperFactory().getObjectTableInfo(this.getPersistentClass());
		this.insertHistory(param);
		Object auditId = this.logAudit(new ParamMap(param).put("action", "update"));
		if(auditId != null) {
			param.put(objectTable.getAuditId(), auditId);
		}
		return this.getNamedParameterJdbcTemplate().update(this.getSqlGenerator().generateUpdate(objectTable, param), param);
	}
	
	public int update(Map<String,Object> values,Map<String,Object> filter) {//TODO Optimistic locking
		this.insertHistory(filter);
		Object auditId = this.logAudit(new ParamMap("values",values).put("filter", filter).put("action", "update"));
		if(auditId != null) {
			ObjectTableInfo objectTable = getMapperFactory().getObjectTableInfo(this.getPersistentClass());
			values.put(objectTable.getAuditId(), auditId);
		}
		String sql = getUpdateSQL( values, filter);
		values.putAll(filter);
		return this.getNamedParameterJdbcTemplate().update(sql, values);
	}
	
	protected String getUpdateSQL(Map<String,Object> values,Map<String,Object> filter) {
		return getSqlGenerator().generateUpdate(
				getMapperFactory().getObjectTableInfo(this.getPersistentClass()), values,filter);
	}
	
	protected String getRemoveByIdSQL() {
		ObjectTableInfo objectTable = getMapperFactory().getObjectTableInfo(this.getPersistentClass());
		return objectTable.getRemoveByIdSql();
	}
	
	public int remove(Object...tableKey ) {
		ObjectTableInfo objectTable = getMapperFactory().getObjectTableInfo(this.getPersistentClass());
		ParamMap param = new ParamMap(objectTable.getIdName(), tableKey);
		this.insertHistory(param);
		Object auditId = this.logAudit(new ParamMap(param).put("action", "remove"));
		return this.getNamedParameterJdbcTemplate().update(
				getRemoveByIdSQL(),
				param);
	}

	@Override
	protected void checkDaoConfig() throws IllegalArgumentException {		
		super.checkDaoConfig();
		if(this.auditStrategy == null) {
			ObjectTableInfo auditObjectTable=this.getMapperFactory().getObjectTableInfo(this.getPersistentClass()).getAuditInfo();
			if(auditObjectTable != null) {
				AuditStrategyImpl audit = new AuditStrategyImpl<Audit>();
				audit.setPersistentClass( auditObjectTable.getIdType());
				audit.setMapperFactory(MapperFactory.getInstance());
				this.auditStrategy = audit; 
			}
			
		}	
	}
}
