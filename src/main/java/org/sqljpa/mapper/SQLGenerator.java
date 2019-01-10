package org.sqljpa.mapper;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public abstract class SQLGenerator {
	private static SQLGenerator instance;
	
	public static SQLGenerator getInstance() {
		if(instance == null) {
			instance = new SQLGenerator() {};
		}
		return instance;
	}
	public String generateInsert(ObjectTableInfo objTbl, boolean isbatch) {
		Set<Entry<String, FieldMapper>> entrySet = objTbl.getColumnToPropertyMap().entrySet();
		StringBuilder colsb = new StringBuilder();
		StringBuilder parsb = new StringBuilder();
		for (Entry<String, FieldMapper> entry : entrySet) {
			FieldMapper field = entry.getValue();
			if(!field.isReadonly()) {				
				if (colsb.length() > 0) {
					colsb.append(", ");
					parsb.append(", ");
				}
				colsb.append(field.getColumn());
				if(field.getName().equals(objTbl.getIdName()) &&  isbatch) {
					parsb.append(objTbl.getSequenceFunction());
				}else {
					parsb.append(":" + field.getName());
				}				
			}			
		}
		StringBuilder result = new StringBuilder("INSERT INTO ");
		result.append(objTbl.getTableName()).append("(").append(colsb).append(") VALUES (").append(parsb).append(") ;");
		return result.toString();
	}
	
	public String generateUpdate(ObjectTableInfo objTbl,Map<String,Object> param) {		
		StringBuilder result = new StringBuilder("UPDATE ");
		result.append(objTbl.getTableName()).append( " SET " );
		Set<Entry<String, Object>> entrySet = param.entrySet();
		boolean first = true;
		for (Entry<String, Object> entry : entrySet) {
			FieldMapper field = objTbl.getColumnToPropertyMap().get(entry.getKey());
			if(!field.getName().equals(objTbl.getIdName())) {
				if(!first) {
					result.append(", ");
				}
				first = false;				
				result.append(field.getColumn()).append(" = :").append(entry.getKey());
			}			
		}		
		result.append(" WHERE ").append(objTbl.getIdColumn()).append(" IN ( :").append(objTbl.getIdName()).append(" )");
		return result.toString();
	}
	
	public String generateWhereClause(ObjectTableInfo objTbl,Map<String,Object> filter) {
		if(filter == null) {
			return "";
		}
		StringBuilder result = new StringBuilder();
		Set<Entry<String, Object>> filterSet = filter.entrySet();
		boolean first = true;
		for(Entry<String, Object> entry:filterSet) {
			FieldMapper field = objTbl.getColumnToPropertyMap().get(entry.getKey());
			if(field == null) {
				continue;
			}
			if(!first) {
				result.append(" AND ");
			}
			first = false;			
			if(field.getName().equals(objTbl.getIdName())) {
				result.append(field.getColumn()).append(" IN (:").append(entry.getKey()).append(" ) ");
			}else {
				result.append(field.getColumn()).append(" = :").append(entry.getKey());
			}			
		}
		if(result.length() > 0) {
			return  new StringBuilder(" WHERE ").append(result).toString();
		}
		return "";
	}
	
	public String generateHistory(ObjectTableInfo objTbl,Map<String,Object> filter) {
		Set<Entry<String, FieldMapper>> fieldSet = objTbl.getColumnToPropertyMap().entrySet();
		StringBuilder cols = new StringBuilder();
		boolean first = true;
		for(Entry<String, FieldMapper> fieldEntry:fieldSet) {
			FieldMapper field = fieldEntry.getValue();			
			if(!first) {
				cols.append(",");
			}
			first = false;
			cols.append(field.getColumn());
		}
		StringBuilder result = new StringBuilder("INSERT INTO ");
		result.append(objTbl.getHistoryTable());
		result.append("(").append(cols).append(")");
		result.append(" SELECT  ").append(cols).append(" FROM ").append(objTbl.getTableName()).append(" ");
		result.append(generateWhereClause(objTbl,filter));
		return result.toString();
	}
	
	public String generateUpdate(ObjectTableInfo objTbl,Map<String,Object> values, Map<String,Object> filter) {		
		StringBuilder result = new StringBuilder("UPDATE ");
		result.append(objTbl.getTableName()).append( " SET " );
		Set<Entry<String, Object>> valuesSet = values.entrySet();
		boolean first = true;
		for (Entry<String, Object> entry : valuesSet) {
			FieldMapper field = objTbl.getColumnToPropertyMap().get(entry.getKey());
			if(field == null) {
				continue;
			}
			if(!field.getName().equals(objTbl.getIdName())) {
				if(!first) {
					result.append(", ");
				}
				first = false;				
				result.append(field.getColumn()).append(" = :").append(entry.getKey());
			}			
		}
		result.append(generateWhereClause(objTbl,filter));
		return result.toString();
	}
	
	public String generateRemoveById(ObjectTableInfo objTbl) {
		StringBuilder result = new StringBuilder("DELETE FROM ");
		result.append(objTbl.getTableName()).append(" WHERE ")
		.append(objTbl.getIdColumn()).append(" IN ( :").append(objTbl.getIdName()).append(" ) ");
		return result.toString();
	}
	
	public String generateSelect(ObjectTableInfo objTbl,Map<String,Object> paramMap) {
		StringBuilder result = new StringBuilder("SELECT * FROM ");
		result.append(objTbl.getTableName()).append(" ").append(this.generateWhereClause(objTbl, paramMap));
		return result.toString();
	}
	
	public String generateSingleSelect(ObjectTableInfo objTbl) {
		StringBuilder result = new StringBuilder("SELECT * FROM ");
		result.append(objTbl.getTableName()).append(" WHERE ").append(objTbl.getIdColumn()).append("=?");
		return result.toString();
	}
	
}
