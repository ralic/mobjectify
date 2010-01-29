package com.google.appengine.api.datastore;

import java.io.Serializable;

/**
 * GWT emulation class.
 */
public class Email implements Serializable, Comparable<Email>
{
	private String email;

	private Email()
	{
	}

	public Email(String email)
	{
		if (email == null)
		{
			throw new NullPointerException("email must not be null");
		}
		else
		{
			this.email = email;
		}
	}

	public String getEmail()
	{
		return email;
	}

	public int compareTo(Email e)
	{
		return email.compareTo(e.email);
	}

	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Email other = (Email) o;
		return email.equals(other.email);
	}

	public int hashCode()
	{
		return email.hashCode();
	}

	public String toString()
	{
		return email;
	}
}
