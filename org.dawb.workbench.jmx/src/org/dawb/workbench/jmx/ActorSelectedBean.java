/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawb.workbench.jmx;


/**
 * Bean which holds data for selecting an actor.
 * 
 * Also used for port highlighting when in debug mode.
 * 
 * @author Matthew Gerring
 *
 */
public class ActorSelectedBean extends ActorBean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 334645527977609959L;
	
	
	private String  resourcePath;
	private boolean isSelected;
	private int     colorCode;
	
	private String  portName;
	private int     portColorCode;
	
	public ActorSelectedBean() {
		
	}
	
	public ActorSelectedBean(String resourcePath, String actorName, boolean isSelected, int colorCode) {
		setResourcePath(resourcePath);
		setActorName(actorName);
		setSelected(isSelected);
		setColorCode(colorCode);
	}
	
	public String getResourcePath() {
		return resourcePath;
	}
	public void setResourcePath(String resourcePath) {
		this.resourcePath = resourcePath;
	}
	public boolean isSelected() {
		return isSelected;
	}
	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}
	public int getColorCode() {
		return colorCode;
	}
	public void setColorCode(int colorCode) {
		this.colorCode = colorCode;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + colorCode;
		result = prime * result + (isSelected ? 1231 : 1237);
		result = prime * result + portColorCode;
		result = prime * result
				+ ((portName == null) ? 0 : portName.hashCode());
		result = prime * result
				+ ((resourcePath == null) ? 0 : resourcePath.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ActorSelectedBean other = (ActorSelectedBean) obj;
		if (colorCode != other.colorCode)
			return false;
		if (isSelected != other.isSelected)
			return false;
		if (portColorCode != other.portColorCode)
			return false;
		if (portName == null) {
			if (other.portName != null)
				return false;
		} else if (!portName.equals(other.portName))
			return false;
		if (resourcePath == null) {
			if (other.resourcePath != null)
				return false;
		} else if (!resourcePath.equals(other.resourcePath))
			return false;
		return true;
	}

	public String getPortName() {
		return portName;
	}

	public void setPortName(String portName) {
		this.portName = portName;
	}

	public int getPortColorCode() {
		return portColorCode;
	}

	public void setPortColorCode(int portColorCode) {
		this.portColorCode = portColorCode;
	}

}
