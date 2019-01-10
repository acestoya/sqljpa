package org.sqljpa.util;

import static java.util.Locale.ENGLISH;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.beanutils.PropertyUtils;

public class SQLUtility {
public static void setProperty(Object bean, String name, Object value) {
	try {
		PropertyUtils.setProperty(bean, name, value);
	} catch (IllegalAccessException e) {
		throw new RuntimeException(e);
	} catch (InvocationTargetException e) {
		throw new RuntimeException(e);
	} catch (NoSuchMethodException e) {
		throw new RuntimeException(e);
	}
 }

public static Object getProperty(Object bean, String name) {
	try {
		return PropertyUtils.getProperty(bean, name);
	} catch (IllegalAccessException e) {
		throw new RuntimeException(e);
	} catch (InvocationTargetException e) {
		throw new RuntimeException(e);
	} catch (NoSuchMethodException e) {
		throw new RuntimeException(e);
	}	
 }
public static String capitalize(String name) {
	return name.substring(0, 1).toUpperCase(ENGLISH) + name.substring(1);
}
}
