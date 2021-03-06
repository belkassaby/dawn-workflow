/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.passerelle.tools.image;

import java.util.ArrayList;
import java.util.List;

import org.dawb.workbench.jmx.UserPlotBean;
import org.dawnsci.passerelle.tools.AbstractBatchTool;
import org.eclipse.dawnsci.analysis.dataset.impl.function.MapToRotatedCartesian;
import org.eclipse.dawnsci.analysis.dataset.roi.ROIBase;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.DoubleDataset;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.Maths;

import ptolemy.kernel.util.NamedObj;

/**
 * This class does image normalisation in the same way that the ImageNormalisationTool does.
 * 
 * @author ssg37927
 *
 */
public class ImageARPESRemapBatchTool extends AbstractBatchTool {

	@Override
	public UserPlotBean process(final UserPlotBean bean, final NamedObj parent) throws Exception {

		final UserPlotBean upb = (UserPlotBean)bean.getToolData();
		final ROIBase roi = (ROIBase) upb.getRois().get("mapping_roi");
		final Dataset kParallel = DatasetFactory.createFromObject(upb.getData().get("kParallel"));
		final Dataset kParaAxis = DatasetFactory.createFromObject(upb.getData().get("kParaAxis"));
		Dataset auxiliaryData = null;
		if (upb.getData().containsKey("auxiliaryData")) {
			auxiliaryData = DatasetFactory.createFromObject(upb.getData().get("auxiliaryData"));
		}
		final Dataset remapped_energy = DatasetFactory.createFromObject(upb.getData().get("remapped_energy"));
		
		final List<IDataset> dataList = getPlottableData(bean);
		if (dataList==null || dataList.size()<1) throw new Exception("No data found for tool "+getBatchToolId());

		final IDataset data = dataList.get(0);
		if (data.getRank()!=2) throw new Exception("Can only use "+getBatchToolId()+" with 2D data!");

		List<IDataset> originalAxes = getAxes(bean);
		//TODO put in some checking here.

		IDataset correctedData = data;
		List<IDataset> correctedAxes = originalAxes;
		
		// Do the processing here if required
		if (auxiliaryData != null) {
			Dataset newEnergyAxis = Maths.subtract(originalAxes.get(0), auxiliaryData.mean());
			Dataset differences = Maths.subtract(auxiliaryData, auxiliaryData.mean());
			
			double meanSteps = (originalAxes.get(0).max().doubleValue()-originalAxes.get(0).min().doubleValue())/(float)originalAxes.get(0).getShape()[0];
			
			Dataset differenceInts = Maths.floor(Maths.divide(differences, meanSteps));
			
			correctedData = DatasetFactory.zeros(DoubleDataset.class, data.getShape());
			for(int y = 0; y < correctedData.getShape()[0]; y++) {
				int min = Math.max(differenceInts.getInt(y), 0);
				int max = Math.min(correctedData.getShape()[1]+differenceInts.getInt(y), correctedData.getShape()[1]);
				int ref = 0;
				for(int xx = min; xx < max; xx++) {
					correctedData.set(data.getObject(y,xx), y,ref);
					ref++;
				}
			}
			
			//correctedData = InterpolatorUtils.remapOneAxis((Dataset) data, 1, (Dataset) auxiliaryData, (Dataset) originalAxes.get(0), newEnergyAxis);
			correctedAxes = new ArrayList<IDataset>();
			correctedAxes.add(newEnergyAxis.clone());
			correctedAxes.add(originalAxes.get(1).clone());
		}
		
		// Get the data ROI
		MapToRotatedCartesian map = new MapToRotatedCartesian((RectangularROI)roi);
		Dataset dataRegion = map.value(correctedData).get(0);
		
		// prepare the results
		//Dataset remappedRegion = InterpolatorUtils.remapAxis(dataRegion, 0, kParallel, kParaAxis);
		Dataset remappedRegion = dataRegion;
		
		// We set the same trace data and regions as would
		// be there if the fitting had run in the ui.
		final UserPlotBean ret = bean.clone();
		
		ret.addList("remapped", remappedRegion.clone());
		ret.addList("remapped_energy", remapped_energy);
		ret.addList("remapped_k_parallel", kParaAxis);

		return ret;
	}


}
