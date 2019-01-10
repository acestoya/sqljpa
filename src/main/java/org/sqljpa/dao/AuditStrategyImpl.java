package org.sqljpa.dao;

import java.lang.reflect.ParameterizedType;
import java.util.Map;

import org.sqljpa.mapper.MapperFactory;
import org.sqljpa.mapper.ObjectTableInfo;
import org.sqljpa.util.AuditLogRequestObject;
import org.sqljpa.util.SQLUtility;

/**
 * @author acest
 *
 */
public class AuditStrategyImpl<T> extends SqlBaseDao<T> implements AuditStrategy {

	/**
	 * @param objectTable
	 * @param auditId
	 * @param param
	 * @return
	 */
	public Object getAuditObj(ObjectTableInfo objectTable,Object auditId,Map<String,Object> param) {
		ObjectTableInfo auditInfo = objectTable.getAuditInfo();
		Audit auditObj=null;
		if(auditInfo != null ) {
			try {
				auditObj = (Audit)auditInfo.getEntityClass().newInstance();
				SQLUtility.setProperty(auditObj, auditInfo.getIdName(),auditId);
				auditObj.setTableName(objectTable.getTableName());
				auditObj.setAction((String)param.get("action"));
				auditObj = this.populateAuditObject(auditObj, param);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}			
		}
		return auditObj;
	}
	
	/**
	 * Override this method for audit trail implementation
	 * @param obj
	 * @param param
	 * @return
	 */
	public Audit populateAuditObject(Audit obj, Map<String,Object> param) {	
		AuditLogRequestObject.getRequestObject();
		return obj;
	}
	

	public Object logAudit(ObjectTableInfo objectTable, Map<String,Object> param) {		
		ObjectTableInfo auditInfo = objectTable.getAuditInfo();
		if(auditInfo != null ) {
			Object auditId = this.getJdbcTemplate().queryForObject(auditInfo.getSequenceGenerator(), auditInfo.getIdType());
			Object auditObj = this.getAuditObj(objectTable, auditId, param);
			this.getNamedParameterJdbcTemplate().update(objectTable.getAuditInfo().getInsertSql(), this.getParam(auditObj, false));
			return auditId;
		}		
		return null;
	}
	
		
	@Override
	public int logHistory(ObjectTableInfo objectTable, Map<String, Object> param) {		
		if((objectTable.getHistoryTable() != null) 
				&& (!objectTable.getHistoryTable().isEmpty())) {
			return this.getNamedParameterJdbcTemplate().update(this.getSqlGenerator().generateHistory(objectTable, param), param);
		}
		return 0;
	}
	
}
