package org.sqljpa.annotation;

public @interface VersionColumn {
	String name() default "";

	String getValueFrom() default "";
}
