package org.sqljpa.mapper;

public class CollectionFieldMapper extends FieldMapper{
	private ObjectTableInfo collectionInfo;	
	private Class itemType;	
	private Class collectionType;
	private String columnPrefix;
	private String columnKey;	
	private CollectionFieldMapper nestedNativeType;
	public ObjectTableInfo getCollectionInfo() {
		return collectionInfo;
	}
	public void setCollectionInfo(ObjectTableInfo collectionInfo) {
		this.collectionInfo = collectionInfo;
	}
	public String getColumnPrefix() {
		return columnPrefix;
	}
	public void setColumnPrefix(String columnPrefix) {
		this.columnPrefix = columnPrefix;
	}
	public Class getItemType() {
		return itemType;
	}
	public void setItemType(Class itemType) {
		this.itemType = itemType;
	}
	public String getColumnKey() {
		return columnKey;
	}
	public void setColumnKey(String columnKey) {
		this.columnKey = columnKey;
	}
	public Class getCollectionType() {
		return collectionType;
	}
	public void setCollectionType(Class collectionType) {
		this.collectionType = collectionType;
	}
	public CollectionFieldMapper getNestedNativeType() {
		return nestedNativeType;
	}
	public void setNestedNativeType(CollectionFieldMapper nestedNativeType) {
		this.nestedNativeType = nestedNativeType;
	}	
	
}
