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

/**
 * Objects inheriting from {@code TypedPoolObject} can be obtained and put back into the {@link StaticClassPool}.
 *
 * @author Thaedrik [thaedrik@codestorming.org]
 */
public abstract class TypedPoolObject {

	final Class<? extends TypedPoolObject> type;

	/**
	 * When released, this object will be put back into the {@link StaticClassPool}.
	 */
	public final void release() {
		StaticClassPool.putback(this, type);
	}

	/**
	 * Type of pool this object will be put.
	 * <p/>
	 * If the given type is {@code null}, the type of the object will be used.
	 *
	 * @param type Type of pool this object will be put.
	 */
	protected TypedPoolObject(Class<? extends TypedPoolObject> type) {
		this.type = type;
	}
}
