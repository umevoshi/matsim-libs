/* *********************************************************************** *
 * project: org.matsim.*
 * HbefaColdEmissionFactorKey.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.emissions;

/**
 * @author benjamin
 * @author julia
 * 
 **/
class HbefaColdEmissionFactorKey {
	
	private HbefaVehicleCategory hbefaVehicleCategory;
	private Pollutant hbefaComponent;
	private int hbefaParkingTime;
	private int hbefaDistance;
	private HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
	
	public HbefaColdEmissionFactorKey(){
	}

	public HbefaColdEmissionFactorKey(HbefaColdEmissionFactorKey key) {
		this.hbefaVehicleCategory = key.getHbefaVehicleCategory();
		this.hbefaComponent = key.getHbefaComponent();
		this.hbefaParkingTime = key.getHbefaParkingTime();
		this.hbefaDistance = key.getHbefaDistance();
		this.hbefaVehicleAttributes = key.getHbefaVehicleAttributes();
	}


	HbefaVehicleCategory getHbefaVehicleCategory() {
		return hbefaVehicleCategory;
	}

	public void setHbefaVehicleCategory(HbefaVehicleCategory hbefaVehicleCategory) {
		this.hbefaVehicleCategory = hbefaVehicleCategory;
	}

	public Pollutant getHbefaComponent() {
		return hbefaComponent;
	}

	public void setHbefaComponent( Pollutant hbefaComponent ) {
		this.hbefaComponent = hbefaComponent;
	}

	public int getHbefaParkingTime() {
		return hbefaParkingTime;
	}

	public void setHbefaParkingTime(Integer hbefaParkingTime) {
		this.hbefaParkingTime = hbefaParkingTime;
	}

	public int getHbefaDistance() {
		return hbefaDistance;
	}

	public void setHbefaDistance(Integer hbefaDistance) {
		this.hbefaDistance = hbefaDistance;
	}
		
	public HbefaVehicleAttributes getHbefaVehicleAttributes() {
		return hbefaVehicleAttributes;
	}

	public void setHbefaVehicleAttributes(HbefaVehicleAttributes hbefaVehicleAttributes) {
		this.hbefaVehicleAttributes = hbefaVehicleAttributes;
	}

	/* need to implement the "equals" method in order to be able to construct an "equal" key
	 later on (e.g. from data available in the simulation)*/
	@Override
	public boolean equals(Object obj) {
	        if(this == obj) {
	              return true;
	         }
	         if (!(obj instanceof HbefaColdEmissionFactorKey)) {
	                return false; 
	         }
	         HbefaColdEmissionFactorKey key = (HbefaColdEmissionFactorKey) obj;
	         return hbefaVehicleCategory.equals(key.getHbefaVehicleCategory())
	         && hbefaComponent.equals(key.getHbefaComponent())
	         && hbefaParkingTime == (key.getHbefaParkingTime())
	         && hbefaDistance == (key.getHbefaDistance())
	         && hbefaVehicleAttributes.equals(key.getHbefaVehicleAttributes());
	}

	// if "equals" is implemented, "hashCode" also needs to be implemented
	@Override
	public int hashCode(){
		return toString().hashCode();
	} 
	

	// needed for "hashCode" method
	@Override
	public String toString(){
		return hbefaVehicleCategory + "; " 
		+ hbefaComponent + "; "
		+ hbefaParkingTime + "; "
		+ hbefaDistance+ "; "
		+ hbefaVehicleAttributes;
	}
}
