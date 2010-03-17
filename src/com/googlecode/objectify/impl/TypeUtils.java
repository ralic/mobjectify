package com.googlecode.objectify.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Embedded;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.OldName;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Serialized;


/**
 */
public class TypeUtils
{
	/** We do not persist fields with any of these modifiers */
	static final int NOT_SAVED_MODIFIERS = Modifier.FINAL | Modifier.STATIC;
	
	/** A map of the primitive types to their wrapper types */
	static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<Class<?>, Class<?>>();
	static {
		PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
		PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
		PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
		PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
		PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
		PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
		PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
	}
	
	/**
	 * Simple container that groups the names associated with fields.  Names
	 * will include the actual name of the field.
	 */
	public static class FieldMetadata
	{
		public Set<String> names = new HashSet<String>();
		public Field field;
		
		public FieldMetadata(Field f) { this.field = f; }
	}

	/**
	 * Simple container that groups the names associated with @AlsoLoad methods.
	 */
	public static class MethodMetadata
	{
		public Set<String> names = new HashSet<String>();
		public Method method;
		
		public MethodMetadata(Method meth) { this.method = meth; }
	}

	/**
	 * Throw an IllegalStateException if the class does not have a no-arg constructor.
	 */
	public static <T> Constructor<T> getNoArgConstructor(Class<T> clazz)
	{
		try
		{
			Constructor<T> ctor = clazz.getDeclaredConstructor(new Class[0]);
			ctor.setAccessible(true);
			return ctor;
		}
		catch (NoSuchMethodException e)
		{
			throw new IllegalStateException("There must be a public no-arg constructor for " + clazz.getName(), e);
		}
	}
	
	/**
	 * @return true if the field can be saved (is persistable), false if it
	 *  is static, final, @Transient, etc.
	 */
	public static boolean isSaveable(Field field)
	{
		return !field.isAnnotationPresent(Transient.class)
			&& ((field.getModifiers() & NOT_SAVED_MODIFIERS) == 0);
	}

	/**
	 * If getType() is an array or Collection, returns the component type - otherwise null
	 */
	public static Class<?> getComponentType(Class<?> type, Type genericType)
	{
		if (type.isArray())
		{
			return type.getComponentType();
		}
		else if (Collection.class.isAssignableFrom(type))
		{
			while (genericType instanceof Class<?>)
				genericType = ((Class<?>) genericType).getGenericSuperclass();

			if (genericType instanceof ParameterizedType)
			{
				Type actualTypeArgument = ((ParameterizedType) genericType).getActualTypeArguments()[0];
				if (actualTypeArgument instanceof Class<?>)
					return (Class<?>) actualTypeArgument;
				else if (actualTypeArgument instanceof ParameterizedType)
					return (Class<?>) ((ParameterizedType) actualTypeArgument).getRawType();
				else
					return null;
			}
			else
			{
				return null;
			}
		}
		else	// not array or collection
		{
			return null;
		}
	}
	
	/**
	 * Extend a property path, adding a '.' separator but also checking
	 * for the first element.
	 */
	public static String extendPropertyPath(String prefix, String name)
	{
		if (prefix == null || prefix.length() == 0)
			return name;
		else
			return prefix + '.' + name;
	}
	
	/**
	 * <p>Prepare a collection of the appropriate type and place it on the pojo's field.
	 * The rules are thus:</p>
	 * <ul>
	 * <li>If the field already contains a collection object, it will be returned.
	 * A new instance will not be created.</li>
	 * <li>If the field is a concrete collection type, an instance of the concrete type
	 * will be created.</li>
	 * <li>If the field is Set, a HashSet will be created.</li>  
	 * <li>If the field is SortedSet, a TreeSet will be created.</li>  
	 * <li>If the field is List, an ArrayList will be created.</li>  
	 * </ul>
	 * 
	 * @param collectionField is a Collection-derived field on the pojo.
	 * @param onPojo is the object whose field should be set 
	 */
	@SuppressWarnings("unchecked")
	public static Collection<Object> prepareCollection(Object onPojo, Wrapper collectionField, int size)
	{
		assert Collection.class.isAssignableFrom(collectionField.getType());
		
		Collection<Object> coll = (Collection<Object>)collectionField.get(onPojo);

		if (coll != null)
		{
			return coll;
		}
		else
		{
			if (!collectionField.getType().isInterface())
			{
				coll = (Collection<Object>)TypeUtils.newInstance(collectionField.getType());
			}
			else if (SortedSet.class.isAssignableFrom(collectionField.getType()))
			{
				coll = new TreeSet<Object>();
			}
			else if (Set.class.isAssignableFrom(collectionField.getType()))
			{
				coll = new HashSet<Object>((int)(size * 1.5));
			}
			else if (List.class.isAssignableFrom(collectionField.getType()) || collectionField.getType().isAssignableFrom(ArrayList.class))
			{
				coll = new ArrayList<Object>(size);
			}
		}
		
		collectionField.set(onPojo, coll);
		
		return coll;
	}

	/**
	 * <p>Sets the embedded null indexes property in an entity, which tracks which elements
	 * of a collection are null.  For a base of "foo.bar", the state
	 * property will be "foo.bar^null".  The value, if present, will be a list of indexes
	 * in an embedded collection which are null.</p>
	 * 
	 * <p>If there are no nulls, this property does not need to be set.</p>
	 */
	public static void setNullIndexes(Entity entity, String pathBase, Collection<Integer> value)
	{
		String path = getNullIndexPath(pathBase);
		entity.setUnindexedProperty(path, value);
	}
	
	/**
	 * <p>Gets the embedded null indexes property in an entity.</p>
	 * @return null if there is no such property
	 * @see #setNullIndexes(Entity, String, Collection)
	 */
	@SuppressWarnings("unchecked")
	public static Set<Integer> getNullIndexes(Entity entity, String pathBase)
	{
		String path = getNullIndexPath(pathBase);
		Collection<Number> indexes = (Collection<Number>)entity.getProperty(path);
		if (indexes == null)
		{
			return null;
		}
		else
		{
			// Fucking datastore converts Integers to Longs, but if we're getting this
			// back from the cache then we will get the original Integer.  Evil.
			Set<Integer> result = new HashSet<Integer>();
			for (Number index: indexes)
				result.add(index.intValue());
			
			return result;
		}
	}
	
	/**
	 * @return the path where you will find the null indexes for a base path
	 */
	public static String getNullIndexPath(String pathBase)
	{
		return pathBase + "^null";
	}
	
	/**
	 * @return true if clazz is an array type or a collection type
	 */
	public static boolean isArrayOrCollection(Class<?> clazz)
	{
		return clazz.isArray() || Collection.class.isAssignableFrom(clazz);
	}
	
	/**
	 * Determines if the field is embedded or not.  Today this checks for
	 * an @Embedded annotation, but in the future it could check the type
	 * (or component type) is one of the natively persistable classes.
	 * 
	 * @return true if field is an embedded class, collection, or array.
	 */
	public static boolean isEmbedded(Field field)
	{
		return field.isAnnotationPresent(Embedded.class);
	}
	
	/** Checked exceptions are LAME. */
	public static <T> T newInstance(Class<T> clazz)
	{
		try
		{
			return clazz.newInstance();
		}
		catch (InstantiationException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}
	
	/** Checked exceptions are LAME. */
	public static <T> T newInstance(Constructor<T> ctor)
	{
		try
		{
			return ctor.newInstance();
		}
		catch (InstantiationException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
		catch (InvocationTargetException e) { throw new RuntimeException(e); }
	}

	/** Checked exceptions are LAME. */
	public static Object field_get(Field field, Object obj)
	{
		try
		{
			return field.get(obj);
		}
		catch (IllegalArgumentException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}

	/** Checked exceptions are LAME. */
	public static void field_set(Field field, Object obj, Object value)
	{
		try
		{
			field.set(obj, value);
		}
		catch (IllegalArgumentException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}

	/**
	 * Get all the persistent fields on a class, checking the superclasses as well.
	 * 
	 * @return the fields we load and save, *not* including @Id & @Parent fields.
	 *  All fields will be set accessable, and returned in order starting with superclass
	 *  fields.
	 */
	public static List<FieldMetadata> getPesistentFields(Class<?> clazz)
	{
		List<FieldMetadata> goodFields = new ArrayList<FieldMetadata>();

		getPersistentFields(clazz, goodFields);
		
		return goodFields;
	}
	
	/** Recursive implementation of getPersistentFields() */
	private static void getPersistentFields(Class<?> clazz, List<FieldMetadata> goodFields)
	{
		if (clazz == null || clazz == Object.class)
			return;
		
		getPersistentFields(clazz.getSuperclass(), goodFields);
		
		for (Field field: clazz.getDeclaredFields())
		{
			if (TypeUtils.isSaveable(field)
					&& !field.isAnnotationPresent(Id.class)
					&& !field.isAnnotationPresent(Parent.class))
			{
				if (field.isAnnotationPresent(Embedded.class) && field.isAnnotationPresent(Serialized.class))
					throw new IllegalStateException("Cannot have @Embedded and @Serialized on the same field! Check " + field);

				FieldMetadata metadata = new FieldMetadata(field);
				metadata.names.add(field.getName());
				
				// Now any additional names, either @AlsoLoad or the deprecated @OldName
				AlsoLoad alsoLoad = field.getAnnotation(AlsoLoad.class);
				if (alsoLoad != null)
					if (alsoLoad.value() == null || alsoLoad.value().length == 0)
						throw new IllegalStateException("Illegal value '" + Arrays.toString(alsoLoad.value()) + "' in @AlsoLoad for " + field);
					else
						for (String value: alsoLoad.value())
							if (value == null || value.trim().length() == 0)
								throw new IllegalStateException("Illegal value '" + value + "' in @AlsoLoad for " + field);
							else
								metadata.names.add(value);

				// TODO: delete this code in a subsequent version
				OldName oldName = field.getAnnotation(OldName.class);
				if (oldName != null)
					if (oldName.value() == null || oldName.value().trim().length() == 0)
						throw new IllegalStateException("Illegal value '" + oldName.value() + "' in @OldName for " + field);
					else
						metadata.names.add(oldName.value());
				
				field.setAccessible(true);
				goodFields.add(metadata);
			}
		}
	}
	
	/**
	 * Get all the methods that are appropriate for saving into on this
	 * class and all superclasses.  Validates that @AlsoLoad methods are
	 * properly created (one parameter, not @Embedded).
	 * 
	 * @return all the correctly specified @AlsoLoad and @OldName methods.
	 *  Methods will be set accessable.  Key will be the immediate name in the annotation.
	 */
	public static List<MethodMetadata> getAlsoLoadMethods(Class<?> clazz)
	{
		List<MethodMetadata> goodMethods = new ArrayList<MethodMetadata>();

		getAlsoLoadMethods(clazz, goodMethods);
		
		return goodMethods;
	}
	
	/** Recursive implementation of getAlsoLoadMethods() */
	private static void getAlsoLoadMethods(Class<?> clazz, List<MethodMetadata> goodMethods)
	{
		if (clazz == null || clazz == Object.class)
			return;
		
		getAlsoLoadMethods(clazz.getSuperclass(), goodMethods);
		
		for (Method method: clazz.getDeclaredMethods())
		{
			// This seems like a good idea
			if (method.isAnnotationPresent(Embedded.class))
				throw new IllegalStateException("@Embedded is not a legal annotation for methods");

			MethodMetadata metadata = new MethodMetadata(method);
			
			for (Annotation[] paramAnnotations: method.getParameterAnnotations())
			{
				for (Annotation ann: paramAnnotations)
				{
					if (ann instanceof OldName || ann instanceof AlsoLoad)
					{
						// Method must have only one parameter
						if (method.getParameterTypes().length != 1)
							throw new IllegalStateException("@AlsoLoad methods must have a single parameter. Can't use " + method);
						
						// Parameter cannot be @Embedded
						for (Annotation maybeEmbedded: paramAnnotations)
							if (maybeEmbedded instanceof Embedded)
								throw new IllegalStateException("@Embedded cannot be used on @AlsoLoad methods. The offender is " + method);
						
						// It's good, let's add it
						method.setAccessible(true);
						
						if (ann instanceof AlsoLoad)
						{
							AlsoLoad alsoLoad = (AlsoLoad)ann;
							if (alsoLoad.value() == null || alsoLoad.value().length == 0)
								throw new IllegalStateException("@AlsoLoad must have a value on " + method);
							
							for (String name: alsoLoad.value())
							{
								if (name == null || name.trim().length() == 0)
									throw new IllegalStateException("Illegal value '" + name + "' in @AlsoLoad for " + method);
								
								metadata.names.add(name);
							}
						}
						else if (ann instanceof OldName)
						{
							OldName oldName = (OldName)ann;
							if (oldName.value() == null || oldName.value().trim().length() == 0)
								throw new IllegalStateException("@OldName must have a value on " + method);
							
							metadata.names.add(oldName.value());
						}
					}
				}
			}
			
			goodMethods.add(metadata);
		}
	}

	/**
	 * Get the underlying class for a type, or null if the type is a variable type.
	 * See http://www.artima.com/weblogs/viewpost.jsp?thread=208860
	 */
	public static Class<?> getClass(Type type)
	{
		if (type instanceof Class<?>)
		{
			return (Class<?>)type;
		}
		else if (type instanceof ParameterizedType)
		{
			return getClass(((ParameterizedType)type).getRawType());
		}
		else if (type instanceof GenericArrayType)
		{
			Type componentType = ((GenericArrayType)type).getGenericComponentType();
			Class<?> componentClass = getClass(componentType);
			if (componentClass != null)
			{
				return Array.newInstance(componentClass, 0).getClass();
			}
			else
			{
				return null;
			}
		}
		else
		{
			return null;
		}
	}

	/**
	 * Get the actual type arguments a child class has used to extend a generic base class.
	 * See http://www.artima.com/weblogs/viewpost.jsp?thread=208860
	 * This has additionally been modified to handle base interfaces.
	 * 
	 * @param baseClass the base class (or interface)
	 * @param childClass the child class
	 * @return a list of the raw classes for the actual type arguments.
	 */
	public static <T> List<Class<?>> getTypeArguments(Class<T> baseClass, Class<? extends T> childClass)
	{
		Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
		Type type = childClass;
		// start walking up the inheritance hierarchy until we hit baseClass
		while (!getClass(type).equals(baseClass))
		{
			if (type instanceof Class<?>)
			{
				// there is no useful information for us in raw types, so just keep going.
				Type superType = ((Class<?>)type).getGenericSuperclass();
				
				// It's possible that the baseClass is an interface, not part of the superclass hierarchy.
				// Fortunately we can be guaranteed there is only one of them somewhere in the hierarchy,
				// so we just need to check the type and all the superinterfaces.
				Class<?> superClass = getClass(superType);
				if (baseClass.isAssignableFrom(superClass))
				{
					type = superType;
				}
				else
				{
					// Need to find another option in the interfaces
					Type[] interfaceTypes = ((Class<?>)type).getGenericInterfaces();
					for (int i=0; i<interfaceTypes.length; i++)
					{
						if (baseClass.isAssignableFrom(getClass(interfaceTypes[i])))
						{
							type = interfaceTypes[i];
							break;
						}
					}
				}
			}
			else
			{
				ParameterizedType parameterizedType = (ParameterizedType)type;
				Class<?> rawType = (Class<?>)parameterizedType.getRawType();

				Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
				TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
				for (int i = 0; i < actualTypeArguments.length; i++)
				{
					resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
				}

				if (!rawType.equals(baseClass))
				{
					type = rawType.getGenericSuperclass();
				}
			}
		}

		// finally, for each actual type argument provided to baseClass,
		// determine (if possible) the raw class for that type argument.
		Type[] actualTypeArguments;
		if (type instanceof Class<?>)
		{
			actualTypeArguments = ((Class<?>)type).getTypeParameters();
		}
		else
		{
			actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
		}
		
		List<Class<?>> typeArgumentsAsClasses = new ArrayList<Class<?>>();
		// resolve types by chasing down type variables.
		for (Type baseType : actualTypeArguments)
		{
			while (resolvedTypes.containsKey(baseType))
			{
				baseType = resolvedTypes.get(baseType);
			}
			
			typeArgumentsAsClasses.add(getClass(baseType));
		}
		
		return typeArgumentsAsClasses;
	}
	
	/**
	 * Just like Class.isAssignableFrom(), but does the right thing when considering autoboxing.
	 */
	public static boolean isAssignableFrom(Class<?> to, Class<?> from)
	{
		Class<?> notPrimitiveTo = to.isPrimitive() ? PRIMITIVE_TO_WRAPPER.get(to) : to;
		Class<?> notPrimitiveFrom = from.isPrimitive() ? PRIMITIVE_TO_WRAPPER.get(from) : from;
		
		return notPrimitiveTo.isAssignableFrom(notPrimitiveFrom);
	}
	
}
