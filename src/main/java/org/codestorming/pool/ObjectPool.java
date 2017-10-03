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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Pool of softly reachable objects.
 * <p/>
 * The pool returns pooled objects in priority, if no such object exists a new one is created with the specified {@link
 * ObjectFactory}.
 *
 * @author Thaedrik [thaedrik@codestorming.org]
 */
public class ObjectPool<T> {

	/**
	 * Factory used by the {@link ObjectPool} for creating objects.
	 *
	 * @param <T> The type of object this factory can create.
	 */
	public interface ObjectFactory<T> {

		/**
		 * Creates the object.
		 *
		 * @return the created object.
		 */
		T create();
	}

	private final ObjectFactory<T> objectFactory;

	private final int maxPoolSize;

	// reclaimed objects
	private final Set<SoftReference<T>> reclaimed;

	private final ReferenceQueue<T> queue = new ReferenceQueue<>();

	/**
	 * Creates a new {@code ObjectPool} with the given {@link org.codestorming.pool.ObjectPool.ObjectFactory} and
	 * maximum pool size.
	 * <p/>
	 * The {@code maxPoolSize} is an hint, the pool may contain more objects than the maximum, but it will try to
	 * regulate its size.
	 *
	 * @param objectFactory The factory used to create new objects.
	 * @param maxPoolSize The maximum size of the pool.
	 */
	public ObjectPool(ObjectFactory<T> objectFactory, int maxPoolSize) {
		if (objectFactory == null) {
			throw new NullPointerException("objectFactory cannot be null");
		} // else
		if (maxPoolSize < 0) {
			throw new IllegalArgumentException("The maxPoolSize must be >= 0");
		} // else
		this.maxPoolSize = maxPoolSize;
		reclaimed = new HashSet<>(maxPoolSize);
		this.objectFactory = objectFactory;
	}

	/**
	 * Creates a new {@code ObjectPool} with no maximum pool size, that is, the pool can be filled until the internal
	 * collection is full.
	 *
	 * @param objectFactory The factory used to create new objects.
	 */
	public ObjectPool(ObjectFactory<T> objectFactory) {
		this(objectFactory, 0);
	}

	/**
	 * Obtain an object from the pool if any, or create a new one with the defined factory.
	 *
	 * @return the object.
	 */
	public T get() {
		T obj = null;
		synchronized (reclaimed) {
			checkReferences();
			if (!reclaimed.isEmpty()) {
				Iterator<SoftReference<T>> iter = reclaimed.iterator();
				obj = iter.next().get();
				iter.remove();
			}
		}
		if (obj == null) {
			obj = objectFactory.create();
		}
		return obj;
	}

	/**
	 * Put back the given object into this pool.
	 * <p/>
	 * The object should no more be used.
	 *
	 * @param object the object to put back in the pool.
	 */
	public void putback(T object) {
		if (object != null &&
				(maxPoolSize == 0 && reclaimed.size() < Integer.MAX_VALUE || reclaimed.size() < maxPoolSize)) {
			synchronized (reclaimed) {
				checkReferences();
				reclaimed.add(new SoftReference<>(object, queue));
			}
		}
	}

	/**
	 * Clear this pool.
	 */
	public void clear() {
		synchronized (reclaimed) {
			reclaimed.clear();
		}
	}

	protected void checkReferences() {
		for (Reference<? extends T> ref; (ref = queue.poll()) != null; ) {
			reclaimed.remove(ref);
			System.out.println("object reclaimed by GC");
		}
	}
}
