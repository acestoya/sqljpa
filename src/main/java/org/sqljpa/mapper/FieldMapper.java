package org.sqljpa.mapper;

import java.lang.reflect.Method;

import javax.persistence.AttributeConverter;

public class FieldMapper {
	private String name;
	private String column;
	private Class type;
	private boolean readonly;
	private AttributeConverter converter;
	private Class converterDatabaseType;
	private Class converterEntityType;
	private Method setter;
	private Method getter;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Class getType() {
		return type;
	}

	public void setType(Class type) {
		this.type = type;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	public String getColumn() {
		return column;
	}

	public void setColumn(String column) {
		this.column = column;
	}

	public AttributeConverter getConverter() {
		return converter;
	}

	public void setConverter(AttributeConverter converter) {
		this.converter = converter;
	}
	
	public boolean hasConverter() {
		return (this.converter != null);
	}

	public Method getSetter() {
		return setter;
	}

	public void setSetter(Method setter) {
		this.setter = setter;
	}

	public Method getGetter() {
		return getter;
	}

	public void setGetter(Method getter) {
		this.getter = getter;
	}

	public Class getConverterDatabaseType() {
		return converterDatabaseType;
	}

	public void setConverterDatabaseType(Class converterDatabaseType) {
		this.converterDatabaseType = converterDatabaseType;
	}

	public Class getConverterEntityType() {
		return converterEntityType;
	}

	public void setConverterEntityType(Class converterEntityType) {
		this.converterEntityType = converterEntityType;
	}
	
}
