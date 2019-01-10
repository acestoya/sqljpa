package org.sqljpa.dao;

public interface Audit {
	void setTableName(String tableName);

	String getTableName();

	void setAction(String action);

	String getAction();
}
