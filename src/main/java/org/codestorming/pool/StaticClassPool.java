/*
 * Copyright (c) 2012-2017 Codestorming.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Codestorming - initial API and implementation
 */
package org.codestorming.pool;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Pool of objects stored by type.
 * <p/>
 * The pooled objects must inherit from {@link TypedPoolObject}.
 *
 * @author Thaedrik [thaedrik@codestorming.org]
 * @see ObjectPool
 */
public class StaticClassPool {

	private static final Map<Class<?>, ObjectPool<?>> pools = new HashMap<>();

	private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();

	/**
	 * Get or create an object of the specified type.
	 *
	 * @param type The type of the object to get.
	 * @return the object of the specified type.
	 * @throws IllegalStateException if there is no pool for the specified type.
	 */
	public static <T extends TypedPoolObject> T get(Class<T> type) {
		return getPool(type).get();
	}

	@SuppressWarnings("unchecked")
	static void putback(TypedPoolObject object, Class<? extends TypedPoolObject> type) {
		if (object != null) {
			if (!type.isInstance(object)) {
				throw new ClassCastException(object.getClass().getName() + " can't be cast to " + type.getName());
			} // else
			ObjectPool<TypedPoolObject> pool = (ObjectPool<TypedPoolObject>) getPool(type);
			pool.putback(object);
		}
	}

	private static <T extends TypedPoolObject> ObjectPool<T> getPool(Class<T> type) {
		if (type == null) {
			throw new NullPointerException();
		}// else

		try {
			LOCK.readLock().lock();
			@SuppressWarnings("unchecked") ObjectPool<T> pool = (ObjectPool<T>) pools.get(type);
			if (pool == null) {
				throw new IllegalStateException("No referenced ObjectPool for the type " + type.getName());
			}// else
			return pool;
		} finally {
			LOCK.readLock().unlock();
		}

	}

	/**
	 * Creates a new pool for the given type and by using the specified factory to create new objects of this type.
	 * <p/>
	 * <strong>ATTENTION:</strong> If a pool of this type already exists, it will be cleared and replaced.
	 *
	 * @param type Type of object to create a pool for.
	 * @param objectFactory Factory to create new instances of this type.
	 * @param <T> Type of object to put in the pool.
	 */
	public static <T extends TypedPoolObject> void referencePool(Class<T> type,
			ObjectPool.ObjectFactory<T> objectFactory) {
		ObjectPool<T> newPool = new ObjectPool<>(objectFactory);
		try {
			LOCK.writeLock().lock();
			ObjectPool<?> oldPool = pools.put(type, newPool);
			if (oldPool != null) {
				oldPool.clear();
			}
		} finally {
			LOCK.writeLock().unlock();
		}
	}

	private StaticClassPool() {}
}
