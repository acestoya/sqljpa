package org.sqljpa.dao;

import java.util.Map;

import org.sqljpa.mapper.ObjectTableInfo;

public interface AuditStrategy {
	Object logAudit(ObjectTableInfo objectTable, Map<String, Object> param);

	int logHistory(ObjectTableInfo objectTable, Map<String, Object> param);
}
