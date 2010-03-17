package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.googlecode.objectify.condition.Always;
import com.googlecode.objectify.condition.If;

/**
 * <p>When placed on an entity field, the field will not be written to the datastore.
 * It will, however, be loaded normally.  This is particularly useful in concert with
 * {@code @PostLoad} and {@code @PrePersist} to transform your data.</p>
 * 
 * <p>If passed one or more classes that implement the {@code If} interface, the
 * value will be unsaved only if it tests positive for any of the conditions.  This
 * is a convenient way to prevent storing of default values, potentially saving
 * a significant amount of storage and indexing cost.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Unsaved
{
	Class<? extends If<?>>[] value() default { Always.class };
}