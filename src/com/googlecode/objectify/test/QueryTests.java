/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.PreparedQuery;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Query;
import com.googlecode.objectify.test.entity.Trivial;

/**
 * Tests of various queries
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(QueryTests.class);

	/** */
	Trivial triv1;
	Trivial triv2;
	List<Key<Trivial>> keys;
	
	/** */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();
		
		this.triv1 = new Trivial("foo1", 1);
		this.triv2 = new Trivial("foo2", 2);
		
		List<Trivial> trivs = new ArrayList<Trivial>();
		trivs.add(this.triv1);
		trivs.add(this.triv2);
		
		Objectify ofy = this.fact.begin();
		this.keys = ofy.put(trivs);
	}	
	
	/** */
	@Test
	public void testKeysOnly() throws Exception
	{
		Objectify ofy = this.fact.begin();
		Query<Trivial> q = ofy.query(Trivial.class);
		
		int count = 0;
		for (Key<Trivial> k: q.fetchKeys())
		{
			assert keys.contains(k);
			count++;
		}
		
		assert count == keys.size();
		
		// Just for the hell of it, test the other methods
		for (Key<Trivial> k: q.fetchKeys(1000, 0))
			assert keys.contains(k);
		
		assert q.count() == keys.size();
		
		try
		{
			q.get();
			assert false: "Should not be able to get() when there are multiple results";
		}
		catch (PreparedQuery.TooManyResultsException ex) {}
	}

	/** */
	@Test
	public void testNormalSorting() throws Exception
	{
		Objectify ofy = this.fact.begin();
		Iterator<Trivial> it = ofy.query(Trivial.class).order("someString").iterator();
		
		Trivial t1 = it.next();
		Trivial t2 = it.next();
		
		assert t1.getId().equals(triv1.getId()); 
		assert t2.getId().equals(triv2.getId()); 
	}
	
	/** */
	@Test
	public void testNormalReverseSorting() throws Exception
	{
		Objectify ofy = this.fact.begin();
		Iterator<Trivial> it = ofy.query(Trivial.class).order("-someString").iterator();
		
		// t2 first
		Trivial t2 = it.next();
		Trivial t1 = it.next();
		
		assert t1.getId().equals(triv1.getId()); 
		assert t2.getId().equals(triv2.getId()); 
	}
	
	/** Unfortunately we can only test one way without custom index file */
	@Test
	public void testIdSorting() throws Exception
	{
		Objectify ofy = this.fact.begin();
		Iterator<Trivial> it = ofy.query(Trivial.class).order("id").iterator();
		
		Trivial t1 = it.next();
		Trivial t2 = it.next();
		
		assert t1.getId().equals(triv1.getId()); 
		assert t2.getId().equals(triv2.getId()); 
	}

	/** */
	@Test
	public void testFiltering() throws Exception
	{
		Objectify ofy = this.fact.begin();
		Iterator<Trivial> it = ofy.query(Trivial.class).filter("someString >", triv1.getSomeString()).iterator();
			
		Trivial t2 = it.next();
		assert !it.hasNext();
		assert t2.getId().equals(triv2.getId()); 
	}

	/** */
	@Test
	public void testIdFiltering() throws Exception
	{
		Objectify ofy = this.fact.begin();
		Iterator<Trivial> it = ofy.query(Trivial.class).filter("id >", triv1.getId()).iterator();
		
		Trivial t2 = it.next();
		assert !it.hasNext();
		assert t2.getId().equals(triv2.getId()); 
	}
	
	/** */
	@Test
	public void testQueryToString() throws Exception
	{
		Objectify ofy = this.fact.begin();
		
		Query<Trivial> q1 = ofy.query(Trivial.class).filter("id >", triv1.getId());
		Query<Trivial> q2 = ofy.query(Trivial.class).filter("id <", triv1.getId());
		Query<Trivial> q3 = ofy.query(Trivial.class).filter("id >", triv1.getId()).order("-id");

		assert !q1.toString().equals(q2.toString());
		assert !q1.toString().equals(q3.toString());
	}

	/** */
	@Test
	public void testEmptySingleResult() throws Exception
	{
		Objectify ofy = this.fact.begin();
		
		Query<Trivial> q = ofy.query(Trivial.class).filter("id", 999999);	// no such entity
		assert q.get() == null;
	}

	/**
	 * Tests issue #3:  http://code.google.com/p/objectify-appengine/issues/detail?id=3 
	 */
	@Test
	public void testFetchOptionsWithTimeoutRetries() throws Exception
	{
		this.fact.setDatastoreTimeoutRetryCount(1);
		Objectify ofy = this.fact.begin();
		
		// This used to throw an exception when wrapping the ArrayList in the retry wrapper
		// because we used the wrong classloader to produce the proxy.  Fixed.
    	Iterable<Key<Trivial>> keys = ofy.query(Trivial.class).fetchKeys(10, 0);
    	
    	assert keys != null;
	}
}