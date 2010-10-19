/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.logging.Logger;

import javax.persistence.Embedded;
import javax.persistence.Id;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Unindexed;

/**
 * Tests of @Indexed and @Unindexed
 * 
 * @author Scott Hernandez
 * @author Jeff Schnitzer
 */
public class IndexingTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(IndexingTests.class.getName());

	@Indexed
	public static class LevelTwoIndexedClass 
	{
	   String bar="A";
	}
	public static class LevelTwoIndexedField 
	{
		@Indexed String bar="A"; 
	}

	public static class LevelOne {
	    String foo = "1";
	    @Embedded LevelTwoIndexedClass twoClass = new LevelTwoIndexedClass();
	    @Embedded LevelTwoIndexedField twoField = new LevelTwoIndexedField();
	}

	@Entity @Unindexed 
 	public static class EntityWithEmbedded {
	    @Id Long id;
	    @Embedded LevelOne one = new LevelOne();
	    String prop = "A";
	}

	@SuppressWarnings("unused")
	public static class EmbeddedIndexedPojo
	{
		@Id Long id;

		@Unindexed 				private boolean aProp = true;
		
		@Indexed 	@Embedded 	private IndexedDefaultPojo[] indexed = {new IndexedDefaultPojo()};
		@Unindexed 	@Embedded 	private IndexedDefaultPojo[] unindexed = {new IndexedDefaultPojo()};
					@Embedded 	private IndexedDefaultPojo[] def = {new IndexedDefaultPojo()};

// 		Fundamentally broken; how to test bad-hetro behavior?

//		@Indexed 	@Embedded 	private List indexedHetro = new ArrayList();
//		@Unindexed 	@Embedded 	private List unindexedHetro = new ArrayList();
//					@Embedded 	private List defHetro = new ArrayList();
//		public EmbeddedIndexedPojo(){
//			indexedHetro.add(new IndexedDefaultPojo());
//			indexedHetro.add(new IndexedPojo());
//			
//			unindexedHetro.addAll(indexedHetro);
//			defHetro.addAll(indexedHetro);
//		}
	}

	@SuppressWarnings("unused")
	@Cached
	@Unindexed
	public static class UnindexedPojo
	{
		@Id Long id;
		@Indexed private boolean indexed = true;
		@Unindexed private boolean unindexed = true;
		private boolean def = true;
	}
	
	@SuppressWarnings("unused")
	@Cached
	@Indexed
	public static class IndexedPojo
	{
		@Id Long id;
		@Indexed private boolean indexed = true;
		@Unindexed private boolean unindexed = true;
		private boolean def = true;
	}
	
	@SuppressWarnings("unused")
	@Cached
	public static class IndexedDefaultPojo
	{
		@Id Long id;
		@Indexed private boolean indexed = true;
		@Unindexed private boolean unindexed = true;
		private boolean def = true;
	}
	

	@SuppressWarnings("unused")
	@Cached
	public static class DefaultIndexedChildFromUnindexedPojo extends UnindexedPojo
	{
		@Indexed
		private boolean indexedChild = true;
		@Unindexed
		private boolean unindexedChild = true;
		private boolean defChild = true;
	}

	@SuppressWarnings("unused")
	@Cached
	public static class DefaultIndexedGrandChildFromUnindexedPojo extends DefaultIndexedChildFromUnindexedPojo
	{
		@Indexed
		private boolean indexedGrandChild = true;
		@Unindexed
		private boolean unindexedGrandChild = true;
		private boolean defGrandChild = true;
	}
	
	/** Switches the default from unindexed to indexed */
	@Cached
	@Indexed
	public static class DerivedAndIndexed extends UnindexedPojo
	{
	}

	/** Switches the default back to indexed from unindexed! */
	@Cached
	@Unindexed
	public static class UnindexedAgain extends DerivedAndIndexed
	{
	}
	
	/** */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();
		
		this.fact.register(IndexedDefaultPojo.class);
		this.fact.register(IndexedPojo.class);
		this.fact.register(UnindexedPojo.class);
		this.fact.register(EmbeddedIndexedPojo.class);
		this.fact.register(EntityWithEmbedded.class);
		this.fact.register(DefaultIndexedChildFromUnindexedPojo.class);
		this.fact.register(DefaultIndexedGrandChildFromUnindexedPojo.class);
		this.fact.register(DerivedAndIndexed.class);
		this.fact.register(UnindexedAgain.class);
	}	
	
	/** */
	@Test
	public void testIndexedPojo() throws Exception
	{
		Objectify ofy = this.fact.begin();
		ofy.put(new IndexedPojo());

		assert ofy.query(IndexedPojo.class).filter("indexed =", true).iterator().hasNext();
		assert ofy.query(IndexedPojo.class).filter("def =", true).iterator().hasNext();
		assert !ofy.query(IndexedPojo.class).filter("unindexed =", true).iterator().hasNext();
	}
	/** */
	@Test
	public void testUnindexedPojo() throws Exception
	{
		Objectify ofy = this.fact.begin();
		ofy.put(new UnindexedPojo());

		assert ofy.query(UnindexedPojo.class).filter("indexed =", true).iterator().hasNext();
		assert !ofy.query(UnindexedPojo.class).filter("def =", true).iterator().hasNext();
		assert !ofy.query(UnindexedPojo.class).filter("unindexed =", true).iterator().hasNext();
		
	}
	/** */
	@Test
	public void testIndexedDefaultPojo() throws Exception
	{
		Objectify ofy = this.fact.begin();
		ofy.put(new IndexedDefaultPojo());

		assert ofy.query(IndexedDefaultPojo.class).filter("indexed =", true).iterator().hasNext();
		assert ofy.query(IndexedDefaultPojo.class).filter("def =", true).iterator().hasNext();
		assert !ofy.query(IndexedDefaultPojo.class).filter("unindexed =", true).iterator().hasNext();
		
	}
	/** */
	@Test
	public void testEmbeddedIndexedPojo() throws Exception
	{
		Objectify ofy = this.fact.begin();
		ofy.put(new EmbeddedIndexedPojo());

		assert  ofy.query(EmbeddedIndexedPojo.class).filter("indexed.indexed =", true).iterator().hasNext();
		assert  ofy.query(EmbeddedIndexedPojo.class).filter("indexed.def =", true).iterator().hasNext();
		assert !ofy.query(EmbeddedIndexedPojo.class).filter("indexed.unindexed=", true).iterator().hasNext();
		assert  ofy.query(EmbeddedIndexedPojo.class).filter("def.indexed =", true).iterator().hasNext();
		assert !ofy.query(EmbeddedIndexedPojo.class).filter("def.unindexed =", true).iterator().hasNext();
		assert  ofy.query(EmbeddedIndexedPojo.class).filter("def.def =", true).iterator().hasNext();
		assert !ofy.query(EmbeddedIndexedPojo.class).filter("unindexed.unindexed =", true).iterator().hasNext();
		assert  ofy.query(EmbeddedIndexedPojo.class).filter("unindexed.indexed =", true).iterator().hasNext();
		assert !ofy.query(EmbeddedIndexedPojo.class).filter("unindexed.def =", true).iterator().hasNext();
		
	}
	/** */
	@Test
	public void testEmbeddedGraph() throws Exception
	{
		/*
		 * one.twoClass.bar = "A"
		 * one.twoField.bar = "A"
		 * one.foo = "1"
		 * id = ?
		 * prop = "A"
		 */
		Objectify ofy = this.fact.begin();
		ofy.put(new EntityWithEmbedded());
		
		assert !ofy.query(EntityWithEmbedded.class).filter("prop =", "A").iterator().hasNext();
		assert !ofy.query(EntityWithEmbedded.class).filter("one.foo =", "1").iterator().hasNext();
		assert  ofy.query(EntityWithEmbedded.class).filter("one.twoClass.bar =", "A").iterator().hasNext();
		assert  ofy.query(EntityWithEmbedded.class).filter("one.twoField.bar =", "A").iterator().hasNext();
	}	

	@Test
	public void testDefaultIndexedChildFromUnindexedPojo() throws Exception
	{
		Objectify ofy = fact.begin();
		ofy.put(new DefaultIndexedChildFromUnindexedPojo());

		assert ofy.query(DefaultIndexedChildFromUnindexedPojo.class).filter("indexed =", true).iterator().hasNext();
		assert !ofy.query(DefaultIndexedChildFromUnindexedPojo.class).filter("def =", true).iterator().hasNext();
		assert !ofy.query(DefaultIndexedChildFromUnindexedPojo.class).filter("unindexed =", true).iterator().hasNext();

		assert ofy.query(DefaultIndexedChildFromUnindexedPojo.class).filter("indexedChild =", true).iterator().hasNext();
		assert !ofy.query(DefaultIndexedChildFromUnindexedPojo.class).filter("defChild =", true).iterator().hasNext();
		assert !ofy.query(DefaultIndexedChildFromUnindexedPojo.class).filter("unindexedChild =", true).iterator().hasNext();
	}

	@Test
	public void testDefaultIndexedGrandChildFromUnindexedPojo() throws Exception
	{
		Objectify ofy = fact.begin();
		ofy.put(new DefaultIndexedGrandChildFromUnindexedPojo());

		assert ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("indexed =", true).iterator().hasNext();
		assert !ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("def =", true).iterator().hasNext();
		assert !ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("unindexed =", true).iterator().hasNext();

		assert ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("indexedChild =", true).iterator().hasNext();
		assert !ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("defChild =", true).iterator().hasNext();
		assert !ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("unindexedChild =", true).iterator().hasNext();

		assert ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("indexedGrandChild =", true).iterator().hasNext();
		assert !ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("defGrandChild =", true).iterator().hasNext();
		assert !ofy.query(DefaultIndexedGrandChildFromUnindexedPojo.class).filter("unindexedGrandChild =", true).iterator().hasNext();
	}
	
	/** */
	@Test
	public void testDerivedAndIndexed() throws Exception
	{
		Objectify ofy = fact.begin();
		ofy.put(new DerivedAndIndexed());
		
		assert ofy.query(DerivedAndIndexed.class).filter("def", true).iterator().hasNext();
	}

	/** */
	@Test
	public void testUnindexedAgain() throws Exception
	{
		Objectify ofy = fact.begin();
		ofy.put(new UnindexedAgain());
		
		assert !ofy.query(UnindexedAgain.class).filter("def", true).iterator().hasNext();
	}
}