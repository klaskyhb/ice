/*******************************************************************************
 * Copyright (c) 2015 UT-Battelle, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jordan H. Deyton (UT-Battelle, LLC.) - Initial API and implementation 
 *   and/or initial documentation
 *   
 *******************************************************************************/
package org.eclipse.ice.viz.service.paraview.proxy;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class provides the standard implementation of the
 * {@link IParaViewProxyFactoryRegistry}.
 * <p>
 * This implementation provides an additional feature: If multiple factories
 * support the same extensions, then for each shared extension, the most
 * recently registered factory will be returned in {@link #getProxyFactory(URI)}
 * . Furthermore, when that factory is unregistered, its supported extensions
 * will fall back to the previously registered factory for that extension.
 * </p>
 * 
 * @author Jordan Deyton
 *
 */
public class ParaViewProxyFactoryRegistry implements
		IParaViewProxyFactoryRegistry {

	/**
	 * The map of factories, keyed on supported extensions. We use a list for
	 * each extension so that multiple factories can be registered for each
	 * extension.
	 */
	private final Map<String, List<IParaViewProxyFactory>> factoryMap;

	/**
	 * The default constructor.
	 * <p>
	 * <b>Note:</b> This class should be instantiated by OSGi!
	 * </p>
	 */
	public ParaViewProxyFactoryRegistry() {
		// Initialize the map of factories.
		factoryMap = new HashMap<String, List<IParaViewProxyFactory>>();
	}

	/*
	 * Implements a method from IParaViewProxyFactoryRegistry.
	 */
	@Override
	public boolean registerProxyFactory(IParaViewProxyFactory factory) {
		boolean registered = false;
		if (factory != null) {
			// Add the factory to the *end* of the list of factories for each
			// supported extension.
			for (String extension : factory.getExtensions()) {
				if (extension != null) {
					// Convert it to lower case.
					extension = extension.toLowerCase();

					List<IParaViewProxyFactory> factories = factoryMap
							.get(extension);

					// Since we want to re-register the factory as the most
					// recent one, remove it from the list. We also must
					// generate a new list of factories for the extension if
					// this is the first factory for it.
					if (factories != null) {
						factories.remove(factory);
					} else {
						factories = new ArrayList<IParaViewProxyFactory>();
						factoryMap.put(extension, factories);
					}

					// Finally, add the factory to the list.
					registered |= factories.add(factory);
				}
			}
		}

		// TODO Send this to a logging system.
		// Print out debug output.
		if (registered) {
			System.out.println("ParaViewProxyFactoryRegistry message: " + "\""
					+ factory.getName() + "\" registered.");
		}

		return registered;
	}

	/*
	 * Implements a method from IParaViewProxyFactoryRegistry.
	 */
	@Override
	public boolean unregisterProxyFactory(IParaViewProxyFactory factory) {
		boolean unregistered = false;
		if (factory != null) {
			// Remove the factory from the list of factories for each supported
			// extension.
			for (String extension : factory.getExtensions()) {
				if (extension != null) {
					// Convert it to lower case.
					extension = extension.toLowerCase();

					List<IParaViewProxyFactory> factories = factoryMap
							.get(extension);
					if (factories != null) {
						unregistered |= factories.remove(factory);

						// Remove the list if the extension is no longer
						// supported.
						if (factories.isEmpty()) {
							factoryMap.remove(extension);
						}
					}
				}
			}
		}

		// TODO Send this to a logging system.
		// Print out debug output.
		if (unregistered) {
			System.out.println("ParaViewProxyFactoryRegistry message: " + "\""
					+ factory.getName() + "\" unregistered.");
		}

		return unregistered;
	}

	/*
	 * Implements a method from IParaViewProxyFactoryRegistry.
	 */
	@Override
	public IParaViewProxyFactory getProxyFactory(URI uri) {
		IParaViewProxyFactory factory = null;
		if (uri != null) {
			String extension = null;

			// If possible, determine the extension of the URI. Make it lower
			// case, as case should not matter.
			try {
				String path = uri.getPath();
				extension = path.substring(path.lastIndexOf(".") + 1)
						.toLowerCase();
			} catch (IndexOutOfBoundsException e) {
				// Nothing to do.
			}

			// Get the last factory that was registered fo the extension.
			List<IParaViewProxyFactory> factories = factoryMap.get(extension);
			if (factories != null && !factories.isEmpty()) {
				factory = factories.get(factories.size() - 1);
			}
		}
		return factory;
	}

	/*
	 * Implements a method from IParaViewProxyFactoryRegistry.
	 */
	@Override
	public Set<String> getExtensions() {
		return new TreeSet<String>(factoryMap.keySet());
	}
}
