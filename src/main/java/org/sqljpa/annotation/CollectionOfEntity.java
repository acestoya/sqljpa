package org.sqljpa.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;

@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
public @interface CollectionOfEntity {
	String prefix() default "";
	String column() default "";
	String columnKey() default "";
	Class entity() default String.class;
	Class paramType() default Object.class;
	Class collectionType() default ArrayList.class;
}
