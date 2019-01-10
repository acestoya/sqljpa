package org.sqljpa.mapper;

import java.lang.reflect.Field;
import java.util.Map;

import javax.persistence.AttributeConverter;

public class ObjectTableInfo {
	private String tableName;
	private Map<String, FieldMapper> columnToPropertyMap;
	private String sequenceGenerator;
	private String sequenceFunction;
	private String insertSql;
	private String insertSqlBatch;
	private String removeByIdSql;
	private Class idType;
	private String idName;
	private ObjectTableInfo auditInfo;
	private String historyTable;
	private Class entityClass;
	private String auditId;
	private CollectionFieldMapper collectionField;
	private CollectionFieldMapper mapField;
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public Map<String, FieldMapper> getColumnToPropertyMap() {
		return columnToPropertyMap;
	}
	public void setColumnToPropertyMap(Map<String, FieldMapper> columnToProperty) {
		this.columnToPropertyMap = columnToProperty;
	}
	
	public String getSequenceGenerator() {
		return sequenceGenerator;
	}
	public void setSequenceGenerator(String sequenceGenerator) {
		this.sequenceGenerator = sequenceGenerator;
	}	
	public String getSequenceFunction() {
		return sequenceFunction;
	}
	public void setSequenceFunction(String sequenceFunction) {
		this.sequenceFunction = sequenceFunction;
	}
	public String getInsertSql() {
		return insertSql;
	}
	public void setInsertSql(String insertSql) {
		this.insertSql = insertSql;
	}	
	public String getInsertSqlBatch() {
		return insertSqlBatch;
	}
	public void setInsertSqlBatch(String insertSqlBatch) {
		this.insertSqlBatch = insertSqlBatch;
	}
	public Class getIdType() {
		return idType;
	}
	public void setIdType(Class idType) {
		this.idType = idType;
	}
	public String getIdName() {
		return idName;
	}
	public void setIdName(String idName) {
		this.idName = idName;
	}
	public String getIdColumn() {
		FieldMapper f = this.columnToPropertyMap.get(this.idName);
		if(f!=null) {
			return f.getColumn();
		}
		return null;
	}	
	public String getHistoryTable() {
		return historyTable;
	}
	public void setHistoryTable(String historyTable) {
		this.historyTable = historyTable;
	}
	public ObjectTableInfo getAuditInfo() {
		return auditInfo;
	}
	public void setAuditInfo(ObjectTableInfo auditInfo) {
		this.auditInfo = auditInfo;
	}
	public Class getEntityClass() {
		return entityClass;
	}
	public void setEntityClass(Class entityClass) {
		this.entityClass = entityClass;
	}
	public String getAuditId() {
		return auditId;
	}
	public void setAuditId(String auditId) {
		this.auditId = auditId;
	}
	public String getRemoveByIdSql() {
		return removeByIdSql;
	}
	public void setRemoveByIdSql(String removeByIdSql) {
		this.removeByIdSql = removeByIdSql;
	}
	public CollectionFieldMapper getCollectionField() {
		return collectionField;
	}
	public void setCollectionField(CollectionFieldMapper collectionField) {
		this.collectionField = collectionField;
	}
	public CollectionFieldMapper getMapField() {
		return mapField;
	}
	public void setMapField(CollectionFieldMapper mapField) {
		this.mapField = mapField;
	}
	
}
