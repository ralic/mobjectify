package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.test.entity.Criminal;
import com.googlecode.objectify.test.entity.Name;

/**
 * Tests specifically dealing with nulls in embedded fields and collections
 */
public class EmbeddedNullTests extends TestBase
{
	/** */
	protected Criminal saveAndFetch(Criminal saveMe)
	{
		Objectify ofy = this.fact.begin();
		
		Key<Criminal> key = ofy.put(saveMe);
		
		return ofy.find(key);
	}

	/**
	 * Add an entry to the database that should never come back from null queries.
	 */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();
		
		Criminal avoid = new Criminal();
		avoid.aliases = new Name[] { new Name("Bob", "Dobbs") };
		avoid.moreAliases = Collections.singletonList(new Name("Bob", "Dobbs"));
		this.fact.begin().put(avoid);
	}

	/**
	 * Rule: nulls come back as nulls
	 * Rule: filtering collections filters by contents, so looking for null fails
	 */
	@Test
	public void testNullCollection() throws Exception
	{
		Criminal crim = new Criminal();
		crim.aliases = null;
		crim.moreAliases = null;
		
		Criminal fetched = this.saveAndFetch(crim);
		assert fetched.aliases == null;
		assert fetched.moreAliases == null;
		
		// Now check the queries
		Objectify ofy = this.fact.begin();
		Iterator<Criminal> queried;

		queried = ofy.query(Criminal.class).filter("aliases", null).iterator();
		assert !queried.hasNext();

		queried = ofy.query(Criminal.class).filter("moreAliases", null).iterator();
		assert !queried.hasNext();

		// Potential altenate syntax?
//		queried = ofy.query(Criminal.class).filterNullCollection("aliases").iterator();
//		assert queried.hasNext();
//		assert queried.next().id.equals(fetched.id);
//		assert !queried.hasNext();

//		queried = ofy.query(Criminal.class).filterNullCollection("moreAliases").iterator();
//		assert queried.hasNext();
//		assert queried.next().id.equals(fetched.id);
//		assert !queried.hasNext();
		
//		queried = ofy.query(Criminal.class).filterEmptyCollection("aliases").iterator();
//		assert !queried.hasNext();

//		queried = ofy.query(Criminal.class).filterEmptyCollection("moreAliases").iterator();
//		assert !queried.hasNext();
	}

	/**
	 */
	@Test
	public void testEmptyCollection() throws Exception
	{
		Criminal crim = new Criminal();
		crim.aliases = new Name[0];
		crim.moreAliases = new ArrayList<Name>();
		
		Criminal fetched = this.saveAndFetch(crim);
		assert fetched.aliases != null;
		assert fetched.aliases.length == 0;
		assert fetched.moreAliases != null;
		assert fetched.moreAliases.isEmpty();

		// Now check the queries
		Objectify ofy = this.fact.begin();
		Iterator<Criminal> queried;

		queried = ofy.query(Criminal.class).filter("aliases", null).iterator();
		assert !queried.hasNext();

		queried = ofy.query(Criminal.class).filter("moreAliases", null).iterator();
		assert !queried.hasNext();
		
		// Potential altenate syntax?
//		queried = ofy.query(Criminal.class).filterNullCollection("aliases").iterator();
//		assert !queried.hasNext();

//		queried = ofy.query(Criminal.class).filterNullCollection("moreAliases").iterator();
//		assert !queried.hasNext();
		
//		queried = ofy.query(Criminal.class).filterEmptyCollection("aliases").iterator();
//		assert queried.hasNext();
//		assert queried.next().id.equals(fetched.id);
//		assert !queried.hasNext();

//		queried = ofy.query(Criminal.class).filterEmptyCollection("moreAliases").iterator();
//		assert queried.hasNext();
//		assert queried.next().id.equals(fetched.id);
//		assert !queried.hasNext();
	}

	/**
	 */
	@Test
	public void testCollectionContainingNull() throws Exception
	{
		Criminal crim = new Criminal();
		crim.aliases = new Name[] { null };
		crim.moreAliases = new ArrayList<Name>();
		crim.moreAliases.add(null);
		
		Criminal fetched = this.saveAndFetch(crim);
		assert fetched.aliases != null;
		assert fetched.aliases.length == 1;
		assert fetched.aliases[0] == null;
		
		assert fetched.moreAliases != null;
		assert fetched.moreAliases.size() == 1;
		assert fetched.moreAliases.get(0) == null;

		// Now check the queries
		Objectify ofy = this.fact.begin();
		Iterator<Criminal> queried;
		
		queried = ofy.query(Criminal.class).filter("aliases", null).iterator();
		assert queried.hasNext();
		assert queried.next().id.equals(fetched.id);
		assert !queried.hasNext();

		queried = ofy.query(Criminal.class).filter("moreAliases", null).iterator();
		assert queried.hasNext();
		assert queried.next().id.equals(fetched.id);
		assert !queried.hasNext();

//		queried = ofy.query(Criminal.class).filterNullCollection("aliases").iterator();
//		assert !queried.hasNext();

//		queried = ofy.query(Criminal.class).filterNullCollection("moreAliases").iterator();
//		assert !queried.hasNext();

//		queried = ofy.query(Criminal.class).filterEmptyCollection("aliases").iterator();
//		assert !queried.hasNext();

//		queried = ofy.query(Criminal.class).filterEmptyCollection("moreAliases").iterator();
//		assert !queried.hasNext();
	}
}
