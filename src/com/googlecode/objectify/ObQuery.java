package com.googlecode.objectify;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;



/**
 * <p>This is similar to the datastore Query object, but better understands
 * real class objects - it allows you to filter and sort by the key field
 * normally.</p>
 * 
 * <p>The methods of this class follow the GAE/Python Query class rather than
 * the GAE/Java Query class because the Python version is much more convenient
 * to use.  The Java version seems to have been designed for machines, not
 * humans.  You will appreciate the improvement.</p>
 * 
 * <p>Construct this class by calling {@code ObjectifyFactory.createQuery()}</p>
 * 
 * <p>Note that this class is not generified because it doesn't really help
 * and actually causes some confusion.  Queries can return a variety of different
 * objects and if you call keysOnly(), will even return Key objects.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObQuery
{
	/** We need this for lookups */
	ObFactory factory;
	
	/** We need to track this because it enables the ability to filter/sort by id */
	Class<?> classRestriction;
	
	/** The actual datastore query constructed by this object */
	protected Query actual;
	
	/** */
	protected ObQuery(ObFactory fact) 
	{
		this.factory = fact;
		this.actual = new Query();
	}
	
	/** */
	protected ObQuery(ObFactory fact, Class<?> clazz)
	{
		this.factory = fact;
		this.actual = new Query(this.factory.getKind(clazz));
		
		this.classRestriction = clazz;
	}
	
	/** @return the underlying datastore query object */
	protected Query getActual()
	{
		return this.actual;
	}
	
	/**
	 * <p>Create a filter based on the specified condition and value, using
	 * the same syntax as the Python query class. Examples:</p>
	 * 
	 * <ul>
	 * <li>{@code filter("age >=", age)}</li>
	 * <li>{@code filter("age =", age)}</li>
	 * <li>{@code filter("age", age)} (if no operator, = is assumed)</li>
	 * <li>{@code filter("age !=", age)}</li>
	 * <li>{@code filter("age in", ageList)}</li>
	 * </ul>
	 * 
	 * <p>You can filter on id properties <strong>if</strong> this query is
	 * restricted to a Class<?> and the entity has no @Parent.  If you are
	 * having trouble working around this limitation, please consult the
	 * objectify-appengine google group.</p>
	 * <p>You can <strong>not</strong> filter on @Parent properties.  Use
	 * the {@code ancestor()} method instead.</p>
	 */
	public ObQuery filter(String condition, Object value)
	{
		String[] parts = condition.trim().split(" ");
		if (parts.length < 1 || parts.length > 2)
			throw new IllegalArgumentException("'" + condition + "' is not a legal filter condition");
		
		String prop = parts[0].trim();
		FilterOperator op = (parts.length == 2) ? this.translate(parts[1]) : FilterOperator.EQUAL;

		// If we have a class restriction, check to see if the property is the @Id
		if (this.classRestriction != null)
		{
			EntityMetadata meta = this.factory.getMetadata(this.classRestriction);
			if (meta.isIdField(prop) || meta.isNameField(prop))
			{
				if (meta.hasParentField())
					throw new IllegalStateException("Cannot (yet) filter by @Id fields on entities which have @Parent fields. Tried '" + prop + "' on " + this.classRestriction.getName() + ".");
				
				if (meta.isIdField(prop))
					value = KeyFactory.createKey(meta.getKind(), ((Number)value).longValue());
				else
					value = KeyFactory.createKey(meta.getKind(), value.toString());
				
				prop = "__key__";
			}
		}

		this.actual.addFilter(prop, op, value);
		
		return this;
	}
	
	/**
	 * Converts the textual operator (">", "<=", etc) into a FilterOperator.
	 * Forgiving about the syntax; != and <> are NOT_EQUAL, = and == are EQUAL.
	 */
	protected FilterOperator translate(String operator)
	{
		operator = operator.trim();
		
		if (operator.equals("=") || operator.equals("=="))
			return FilterOperator.EQUAL;
		else if (operator.equals(">"))
			return FilterOperator.GREATER_THAN;
		else if (operator.equals(">="))
			return FilterOperator.GREATER_THAN_OR_EQUAL;
		else if (operator.equals("<"))
			return FilterOperator.LESS_THAN;
		else if (operator.equals("<="))
			return FilterOperator.LESS_THAN_OR_EQUAL;
		else if (operator.equals("!=") || operator.equals("<>"))
			return FilterOperator.NOT_EQUAL;
		else if (operator.toLowerCase().equals("in"))
			return FilterOperator.IN;
		else
			throw new IllegalArgumentException("Unknown operator '" + operator + "'");
	}
	
	/**
	 * <p>Sorts based on a property.  Examples:</p>
	 * 
	 * <ul>
	 * <li>{@code sort("age")}</li>
	 * <li>{@code sort("-age")} (descending sort)</li>
	 * </ul>
	 * 
	 * <p>You can sort on id properties <strong>if</strong> this query is
	 * restricted to a Class<?>.  Note that this is only important for
	 * descending sorting; default iteration is key-ascending.</p>
	 * <p>You can <strong>not</strong> sort on @Parent properties.</p>
	 */
	public ObQuery sort(String condition)
	{
		condition = condition.trim();
		SortDirection dir = SortDirection.ASCENDING;
		
		if (condition.startsWith("-"))
		{
			dir = SortDirection.DESCENDING;
			condition = condition.substring(1).trim();
		}
		
		// Check for @Id field
		if (this.classRestriction != null)
		{
			EntityMetadata meta = this.factory.getMetadata(this.classRestriction);
			if (meta.isIdField(condition) || meta.isNameField(condition))
				condition = "__key__";
		}

		this.actual.addSort(condition, dir);
		
		return this;
	}
	
	/**
	 * Restricts result set only to objects which have the given ancestor
	 * somewhere in the chain.  Doesn't need to be the immediate parent.
	 * 
	 * @param keyOrEntity can be either a Key or an Objectify entity object.
	 */
	public ObQuery ancestor(Object keyOrEntity)
	{
		if (keyOrEntity instanceof Key)
			this.actual.setAncestor((Key)keyOrEntity);
		else
			this.actual.setAncestor(this.factory.createKey(keyOrEntity));
		
		return this;
	}
	
	/**
	 * Makes this ObQuery a keys only query.  The resulting ObPreparedQuery
	 * will only return Key objects.
	 */
	public ObQuery keysOnly()
	{
		this.actual.setKeysOnly();
		return this;
	}
}