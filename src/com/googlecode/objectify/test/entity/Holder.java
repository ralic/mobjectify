/*
 * $Id$
 * $URL$
 */

package com.googlecode.objectify.test.entity;

import javax.persistence.Id;

/**
 * A holder of a <T>hing.
 * 
 * @author Scott Hernandez
 */
public abstract class Holder<T>
{	
	@Id Long id;
	T thing;
	
	/** Default constructor must always exist */
	protected Holder() {}
	protected Holder(T t) {this.thing = t;}
	
	public T getThing()
	{
		return this.thing;
	}
	public void setThing(T t)
	{
		this.thing = t;
	}
}