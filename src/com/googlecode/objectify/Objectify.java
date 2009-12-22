package com.googlecode.objectify;

import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;

/**
 * <p>This interface is similar to DatastoreService, except that instead of working with
 * Entity you work with real typed objects.</p>
 * 
 * <p>Unlike DatastoreService, none of these methods take a Transaction as a parameter.
 * Instead, a transaction (or lack thereof) is associated with a particular instance of
 * this interface when you create it.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Objectify
{
	/**
	 * Performs a batch get, returning your typed objects.
	 * @param keys are the keys to fetch; you can mix and match the types of objects.
	 * @return a empty map if no keys are found in the datastore.
	 * @see DatastoreService#get(Iterable) 
	 */
	<T> Map<Key, T> get(Iterable<Key> keys);
	
	/**
	 * Gets one instance of your typed object.
	 * @throws EntityNotFoundException if the key does not exist in the datastore
	 * @see DatastoreService#get(Key) 
	 */
	<T> T get(Key key) throws EntityNotFoundException;
	
	/**
	 * This is a convenience method, shorthand for get(ObjectifyFactory.createKey(clazz, id)); 
	 * @throws EntityNotFoundException if the key does not exist in the datastore
	 */
	<T> T get(Class<T> clazz, long id) throws EntityNotFoundException;
	
	/**
	 * This is a convenience method, shorthand for get(ObjectifyFactory.createKey(clazz, name)); 
	 * @throws EntityNotFoundException if the key does not exist in the datastore
	 */
	<T> T get(Class<T> clazz, String name) throws EntityNotFoundException;
	
	/**
	 * This is a convenience method that prevents you from having to assemble all the Keys
	 * yourself and calling get(Iterable<Key>).  Note that unlike that method, this method
	 * only deletes a homogeneous set of objects.
	 * 
	 * @param idsOrNames <b>must</b> be of type Iterable<Long> (which translates to id keys)
	 *  or of type Iterable<String> (which translates to name keys).
	 * @throws IllegalArgumentException if ids is not Iterable<Long> or Iterable<String>
	 */
	<T> Map<Key, T> get(Class<T> clazz, Iterable<?> idsOrNames);
	
	/**
	 * Just like the DatastoreService method, but uses your typed object.
	 * If the object has a null key, one will be created.  If the object
	 * has a key, it will overwrite any value formerly stored with that key.
	 * @see DatastoreService#put(com.google.appengine.api.datastore.Entity) 
	 */
	Key put(Object obj);
	
	/**
	 * Just like the DatastoreService method, but uses your typed objects.
	 * If any of the objects have a null key, one will be created.  If any
	 * of the objects has a key, it will overwrite any value formerly stored
	 * with that key.  You can mix and match the types of objects stored.
	 * @see DatastoreService#put(Iterable) 
	 */
	List<Key> put(Iterable<?> objs);
	
	/**
	 * Deletes the specified entity.  The object passed in can be either a Key
	 * or an entity object; if an entity, only the id fields are relevant.
	 */
	public void delete(Object keyOrEntity);

	/**
	 * Deletes the specified keys or entities.  If the parameter is an iterable of
	 * entity objects, only their key fields are relevant.
	 * 
	 * @param keysOrEntities can be either an iterable of Key objects or an iterable of entity
	 *  objects.  They can even be mixed and matched. The result will be one batch delete.
	 * 
	 * @see DatastoreService#delete(Iterable)
	 */
	public void delete(Iterable<?> keysOrEntities);
	
	/**
	 * <p>Prepares a query for execution.  The resulting ObjPreparedQuery allows the result
	 * set to be iterated through in a typesafe way.</p>
	 * 
	 * <p>You should create a query by calling one of the {@code ObjectifyFactory.createQuery()}
	 * methods.</p>
	 * 
	 * <p>Note:  If Query is keysOnly, result will be ObjPreparedQuery<Key>.
	 * This behavior differs from how the underlying DatastoreService works.</p>
	 * 
	 * @see DatastoreService#prepare(Query)
	 */
	<T> ObjPreparedQuery<T> prepare(Query query);
	
	/**
	 * <p>Note that this is *not* the same as {@code DatastoreService.getCurrentTransaction()},
	 * which uses implicit transaction management.  Objectify does not use implicit (thread
	 * local) transactions.</p>
	 * 
	 * @return the transaction associated with this Objectify instance,
	 *  or null if no transaction is associated with this instance.
	 */
	public Transaction getTxn();

	/**
	 * @return the underlying DatastoreService implementation so you can work
	 *  with Entity objects if you so choose.  Also allows you to allocateIds
	 *  or examine thread local transactions.
	 */
	public DatastoreService getDatastore();
	
}