package com.googlecode.objectify.condition;


/**
 * <p>Simple If condition that returns true if the value is a boolean false.  Note
 * that a null is still false.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class IfFalse implements If<Boolean>
{
	@Override
	public boolean matches(Boolean value)
	{
		return value != null && !value;
	}
}