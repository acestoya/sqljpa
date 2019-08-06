package org.sqljpa.util;

import javax.persistence.AttributeConverter;

public class BooleanToYNConverter implements AttributeConverter<Boolean, String>{
	 
	    public String convertToDatabaseColumn(Boolean value) {        
	        return (value != null && value) ? "Y" : "N";            
	        }    

	    
	    public Boolean convertToEntityAttribute(String value) {
	        return "Y".equals(value);
	        }
}
