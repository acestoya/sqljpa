package org.sqljpa.util;

import javax.servlet.http.HttpServletRequest;

public class AuditLogRequestObject {
	private static ThreadLocal<HttpServletRequest> threadLocal = new ThreadLocal<HttpServletRequest>();

	public static HttpServletRequest getRequestObject() {
		return threadLocal.get();
	}

	public static void setRequestObject(HttpServletRequest value) {
		threadLocal.set(value);
	}
	
	public static void cleanup() {
		threadLocal.remove();
	}
}
