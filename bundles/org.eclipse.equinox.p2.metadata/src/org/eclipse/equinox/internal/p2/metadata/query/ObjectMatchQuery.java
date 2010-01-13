/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.query;

import org.eclipse.equinox.p2.query.MatchQuery;

/**
 * Special implementation for use without generic support
 */
public abstract class ObjectMatchQuery extends MatchQuery<Object> {
	public boolean isMatch(Object candidate) {
		return true;
	}
}
