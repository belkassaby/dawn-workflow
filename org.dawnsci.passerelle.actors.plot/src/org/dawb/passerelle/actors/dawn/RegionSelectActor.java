/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawb.passerelle.actors.dawn;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.parameter.roi.ROIParameter;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.actor.ProcessingException;

public class RegionSelectActor extends AbstractDataMessageTransformer {

	private static final long serialVersionUID = 6990781788498677167L;

	public ROIParameter selectionROI;
	public StringParameter roiName;

	public RegionSelectActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		selectionROI = new ROIParameter(this, "selectionROI");
		registerConfigurableParameter(selectionROI);
		roiName = new StringParameter(this, "roiName");
		registerConfigurableParameter(roiName);
	}

	@Override
	protected DataMessageComponent getTransformedMessage(
			List<DataMessageComponent> cache) throws ProcessingException {
		// get the data out of the message, name of the item should be specified
		final Map<String, Serializable>  data = MessageUtils.getList(cache);

		// get the roi out of the message, name of the roi should be specified
		RectangularROI roi = (RectangularROI) selectionROI.getRoi();

		// check the roi list, and see if one exists
		try {
			Map<String, IROI> rois = MessageUtils.getROIs(cache);

			if(rois.containsKey(roiName.getExpression())) {
				if (rois.get(roiName.getExpression()) instanceof RectangularROI) {
					roi = (RectangularROI) rois.get(roiName.getExpression());
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// then get the region
		final int yInc = roi.getPoint()[1]<roi.getEndPoint()[1] ? 1 : -1;
		final int xInc = roi.getPoint()[0]<roi.getEndPoint()[0] ? 1 : -1;

		int yStart = (int) roi.getPoint()[1];
		int xStart = (int) roi.getPoint()[0];
		int yStop = (int) roi.getEndPoint()[1];
		int xStop = (int) roi.getEndPoint()[0];

		// prepare the output message
		DataMessageComponent result = new DataMessageComponent();

		// put all the datasets in for reprocessing
		for (String key : data.keySet()) {
			Dataset dataset = DatasetFactory.createFromObject(data.get(key));
			result.addList(key, dataset);

			if (dataset.getShape().length == 2) {

				Dataset dataRegion = dataset.clone();

				dataRegion = dataRegion.getSlice(new int[] { yStart, xStart },
						new int[] { yStop, xStop },
						new int[] {yInc, xInc});

				dataRegion.setName(dataset.getName()+"_region");
				
				// Return the calculated values
				result.addList(dataRegion.getName(), dataRegion);
			}
			
			// it could be an axis, and we want to scale these as well
			// TODO, this is a dumb way of doing it, but is easy to implement, this should be improved somehow
			if (dataset.getShape().length == 1) {

				Dataset dataRegionX = dataset.clone();
				Dataset dataRegionY = dataset.clone();
				
				try {
					dataRegionX = dataRegionX.getSlice(new int[] { xStart },
						new int[] { xStop },
						new int[] { xInc});
					
					dataRegionX.setName(dataset.getName()+"_regionX");
					result.addList(dataRegionX.getName(), dataRegionX);
				} catch (Exception e) {
					// Ignore, as this is not important
				}
				
				try {
					dataRegionY = dataRegionY.getSlice(new int[] { yStart },
						new int[] { yStop },
						new int[] { yInc});
					
					dataRegionY.setName(dataset.getName()+"_regionY");
					result.addList(dataRegionY.getName(), dataRegionY);
				} catch (Exception e) {
					// Ignore as this is not important
				}
				
			}
		}
		return result;
	}

	@Override
	protected String getOperationName() {
		// TODO Auto-generated method stub
		return null;
	}



}
