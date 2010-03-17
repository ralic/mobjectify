package com.googlecode.objectify.impl.save;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Unindexed;
import com.googlecode.objectify.annotation.Unsaved;
import com.googlecode.objectify.condition.Always;
import com.googlecode.objectify.condition.If;
import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>Most savers are related to a particular type of field.  This provides
 * a convenient base class.</p>
 */
abstract public class FieldSaver implements Saver
{
	String path;
	Field field;
	boolean indexed;
	boolean forcedInherit;	// will any child classes be forced to inherit this indexed state
	If<?>[] unsavedConditions;
	
	/**
	 * @param examinedClass is the class which is being registered (or embedded).  It posesses the field,
	 * but it is not necessarily the declaring class (which could be a base class).
	 * @param inheritedIndexed is whther or not higher level instructions were to index this field.
	 * @param collectionize is whether or not the elements of this field should be stored in a collection;
	 * this is used for embedded collection class fields. 
	 */
	public FieldSaver(String pathPrefix, Class<?> examinedClass, Field field, boolean inheritedIndexed, boolean collectionize)
	{
		if (field.isAnnotationPresent(Indexed.class) && field.isAnnotationPresent(Unindexed.class))
			throw new IllegalStateException("Cannot have @Indexed and @Unindexed on the same field: " + field);
		
		this.field = field;
		
		this.path = TypeUtils.extendPropertyPath(pathPrefix, field.getName());
		
		this.indexed = inheritedIndexed;
		if (field.isAnnotationPresent(Indexed.class))
		{
			this.indexed = true;
			this.forcedInherit = true;
		}
		else if (field.isAnnotationPresent(Unindexed.class))
		{
			this.indexed = false;
			this.forcedInherit = true;
		}
		
		// Now watch out for @Unsaved conditions
		Unsaved unsaved = field.getAnnotation(Unsaved.class);
		if (unsaved != null)
		{
			if (collectionize && (unsaved.value().length != 1 || unsaved.value()[0] != Always.class))
				throw new IllegalStateException("You cannot use @Unsaved with a condition within @Embedded collections; check the field " + this.field);
			
			this.unsavedConditions = new If<?>[unsaved.value().length];
			
			for (int i=0; i<unsaved.value().length; i++)
			{
				Class<? extends If<?>> ifClass = unsaved.value()[i];
				this.unsavedConditions[i] = this.createIf(ifClass, examinedClass);

				// Sanity check the generic If class type to ensure that it matches the actual type of the field.
				Class<?> typeArgument = TypeUtils.getTypeArguments(If.class, ifClass).get(0);
				if (!TypeUtils.isAssignableFrom(typeArgument, field.getType()))
					throw new IllegalStateException("Cannot use If class " + ifClass.getName() + " on " + field
							+ " because you cannot assign " + field.getType().getName() + " to " + typeArgument.getName());
			}
		}
	}
	
	/** */
	private If<?> createIf(Class<? extends If<?>> ifClass, Class<?> examinedClass)
	{
		try
		{
			Constructor<? extends If<?>> ctor = TypeUtils.getConstructor(ifClass, Class.class, Field.class);
			return TypeUtils.newInstance(ctor, examinedClass, this.field);
		}
		catch (IllegalStateException ex)
		{
			try
			{
				Constructor<? extends If<?>> ctor = TypeUtils.getNoArgConstructor(ifClass);
				return TypeUtils.newInstance(ctor);
			}
			catch (IllegalStateException ex2)
			{
				throw new IllegalStateException("The If<?> class " + ifClass.getName() + " must have a no-arg constructor or a constructor that takes one argument of type Field.");
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.save.Saver#save(java.lang.Object, com.google.appengine.api.datastore.Entity)
	 */
	@Override
	@SuppressWarnings("unchecked")
	final public void save(Object pojo, Entity entity)
	{
		Object value = TypeUtils.field_get(this.field, pojo);
		
		if (this.unsavedConditions != null)
		{
			for (int i=0; i<this.unsavedConditions.length; i++)
				if (((If<Object>)this.unsavedConditions[i]).matches(value))
					return;
		}
		
		this.saveValue(value, entity);
	}
	
	/**
	 * Actually save the value in the entity.  This is the real value, already obtained
	 * from the POJO and checked against the @Unsaved mechanism..
	 */
	abstract protected void saveValue(Object value, Entity entity);

	/** 
	 * Sets property on the entity correctly for the values of this.path and this.indexed.
	 */
	protected void setEntityProperty(Entity entity, Object value)
	{
		if (this.indexed)
			entity.setProperty(this.path, value);
		else
			entity.setUnindexedProperty(this.path, value);
	}
}
