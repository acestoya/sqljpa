package org.sqljpa.util;

import java.util.HashMap;
import java.util.Map;

public class ParamMap extends HashMap<String, Object> {
	public ParamMap() {
		super();
	}
	
	public ParamMap(Map m) {
		super();
		this.putAll(m);
	}

	public ParamMap(String key, Object value) {
		super();
		this.put(key, value);
	}

	@Override
	public ParamMap put(String key, Object value) {
		super.put(key, value);
		return this;
	}
	
}
