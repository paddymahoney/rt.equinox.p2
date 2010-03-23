/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata.query;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IUpdateDescriptor;
import org.eclipse.equinox.internal.provisional.p2.query.Query;

public class UpdateQuery extends Query {
	private IInstallableUnit updateFrom;

	public UpdateQuery(IInstallableUnit updateFrom) {
		this.updateFrom = updateFrom;
	}

	public boolean isMatch(Object obj) {
		if (!(obj instanceof IInstallableUnit))
			return false;
		IInstallableUnit candidate = (IInstallableUnit) obj;
		IUpdateDescriptor descriptor = candidate.getUpdateDescriptor();
		if (descriptor != null && descriptor.isUpdateOf(updateFrom) && updateFrom.getVersion().compareTo(candidate.getVersion()) < 0)
			return true;
		return false;
	}
}