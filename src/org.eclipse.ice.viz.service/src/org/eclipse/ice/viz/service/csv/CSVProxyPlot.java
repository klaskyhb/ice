/*******************************************************************************
 * Copyright (c) 2015 UT-Battelle, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jordan Deyton - Initial API and implementation and/or initial documentation
 *   
 *******************************************************************************/
package org.eclipse.ice.viz.service.csv;

import java.util.List;

import org.eclipse.ice.viz.service.IPlot;
import org.eclipse.ice.viz.service.IPlotListener;
import org.eclipse.ice.viz.service.ISeries;
import org.eclipse.ice.viz.service.ProxyPlot;
import org.eclipse.ice.viz.service.ProxySeries;
import org.eclipse.ice.viz.service.styles.XYZSeriesStyle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;

/**
 * This class serves as a proxy for a normal {@link CSVPlot}, which is itself
 * responsible for loading and storing the CSV data. Instances of this class are
 * provided from the {@link CSVVizService} when a plot is created, and it gives
 * full access to customizing a plot without affecting the data source plot.
 * 
 * @author Jordan Deyton
 *
 */
public class CSVProxyPlot extends ProxyPlot implements IPlotListener {

	/**
	 * The currently drawn plot composite.
	 */
	private CSVPlotComposite plotComposite = null;

	/**
	 * Whether or not the current data from the associated {@link CSVPlot} is
	 * loaded.
	 */
	private boolean loaded = false;

	/*
	 * Overrides a method from ProxyPlot.
	 */
	@Override
	protected ProxySeries createProxySeries(ISeries source) {
		ProxySeries series = super.createProxySeries(source);
		XYZSeriesStyle style = (XYZSeriesStyle) source.getStyle();
		series.setStyle((XYZSeriesStyle) style.clone());
		return series;
	}

	/*
	 * Overrides a method from AbstractPlot.
	 */
	@Override
	public Composite draw(Composite parent) throws Exception {

		// If necessary, create a new plot composite.
		if (plotComposite == null) {
			// Check the parent Composite.
			if (parent == null) {
				throw new SWTException(SWT.ERROR_NULL_ARGUMENT, "IPlot error: "
						+ "Cannot draw plot in a null Composite.");
			} else if (parent.isDisposed()) {
				throw new SWTException(SWT.ERROR_WIDGET_DISPOSED,
						"IPlot error: "
								+ "Cannot draw plot in a disposed Composite.");
			}

			// Create a plot composite.
			plotComposite = new CSVPlotComposite(parent, SWT.BORDER);
			plotComposite.setPlot(this);
			// Tell it to update based on the new plot.
			plotComposite.refresh();

			// When the composite is disposed, unset the reference to it so a
			// new composite can be drawn later.
			plotComposite.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					plotComposite = null;
				}
			});
		}
		// Otherwise, ignore the parent argument and refresh.
		else {
			plotComposite.refresh();
		}

		return plotComposite;
	}

	/*
	 * Overrides a method from ProxyPlot.
	 */
	@Override
	public List<String> getCategories() {
		// Try to reload the series data if necessary.
		if (!loaded) {
			reloadSeries();
		}
		return super.getCategories();
	}

	/*
	 * Overrides a method from ProxyPlot.
	 */
	@Override
	public List<ISeries> getDependentSeries(String category) {
		// Try to reload the series data if necessary.
		if (!loaded) {
			reloadSeries();
		}
		return super.getDependentSeries(category);
	}

	/*
	 * Overrides a method from AbstractPlot.
	 */
	@Override
	public ISeries getIndependentSeries() {
		// Try to reload the series data if necessary.
		if (!loaded) {
			reloadSeries();
		}
		return super.getIndependentSeries();
	}

	/*
	 * Implements a method from IPlotListener.
	 */
	@Override
	public void plotUpdated(IPlot plot, String key, String value) {
		// The only event sent out by the CSVPlot signifies that loading has
		// completed. Forces the data to be reloaded the next time it is fetched
		// from client code (which should happen in the refresh).
		loaded = false;
		if (plotComposite != null && !plotComposite.isDisposed()) {
			plotComposite.refresh();
		}
	}

	/*
	 * Overrides a method from AbstractPlot.
	 */
	@Override
	public void redraw() {
		// Use the source CSVPlot's redraw operation, which reloads the file.
		IPlot source = getSource();
		if (source != null) {
			loaded = false;
			source.redraw();
		}
		return;
	}

	/*
	 * Overrides a method from ProxyPlot.
	 */
	@Override
	protected void reloadSeries() {

		IPlot source = getSource();
		if (source != null && ((CSVPlot) source).isLoaded()) {
			// We can now load the proxy series.
			loaded = true;

			// Load the dependent series (and categories) in the default way.
			super.reloadSeries();

			// Enable the first dependent series.
			for (String category : getCategories()) {
				List<ISeries> series = getDependentSeries(category);
				if (series != null && !series.isEmpty()) {
					series.iterator().next().setEnabled(true);
					break;
				}
			}

			// Reset the independent series.
			ISeries sourceSeries = source.getIndependentSeries();
			ProxySeries proxy = createProxySeries(sourceSeries);
			// Enable the series by default.
			proxy.setEnabled(true);
			super.setIndependentSeries(proxy);
		}

		return;
	}

	/*
	 * Overrides a method from ProxyPlot.
	 */
	@Override
	public void setSource(IPlot source) {
		super.setSource(source);

		// Register as a listener to be notified when the data has been
		// reloaded.
		((CSVPlot) source).addPlotListener(this);
	}

}
