/******************************************************************************* 
* Copyright (c) 2008, 2017 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
 *   IBM - ongoing development
******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import java.util.Map;

/**
 * Describes a capability that is exposed by an installable unit. These capabilities
 * can satisfy the dependencies of other installable units, causing the unit
 * providing the dependency to be installed.
 * <p>
 * Instances of this class are handle objects and do not necessarily
 * reflect entities that exist in any particular profile or repository. These handle 
 * objects can be created using {@link MetadataFactory}.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 * @see IRequirement
 */
public interface IProvidedCapability {
	/**
	 * 
	 * @return String the namespace of this capability.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public String getNamespace();

	/**
	 * 
	 * @return String the attribute stored under a key equal to {@link #getNamespace()} attribute of this capability.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public String getName();

	/**
	 * 
	 * @return String the special <code>version</code> attribute of this capability.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public Version getVersion();

	/**
	 * 
	 * @return A full description of this capability
	 * @noreference This method is not intended to be referenced by clients.
	 * @since 2.4
	 */
	public Map<String, Object> getAttributes();

	/**
	 * Returns whether this provided capability is equal to the given object.
	 * 
	 * This method returns <i>true</i> if:
	 * <ul>
	 *  <li> Both this object and the given object are of type IProvidedCapability
	 *  <li> The result of <b>getNamespace()</b> on both objects are equal
	 *  <li> The result of <b>getAttributes()</b> on both objects are equal
	 * </ul> 
	 */
	@Override
	public boolean equals(Object other);

}