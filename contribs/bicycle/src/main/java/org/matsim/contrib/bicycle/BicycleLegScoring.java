/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.bicycle;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.bicycle.run.BicycleConfigGroup;
import org.matsim.contrib.bicycle.run.BicycleTravelDisutility;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;

/**
 * This is a re-implementation of the original CharyparNagel function, based on a
 * modular approach.
 * @see <a href="http://www.matsim.org/node/263">http://www.matsim.org/node/263</a>
 * @author rashid_waraich
 */
public class BicycleLegScoring implements org.matsim.core.scoring.SumScoringFunction.LegScoring, org.matsim.core.scoring.SumScoringFunction.ArbitraryEventScoring {
	// yyyy URL in above javadoc is broken.  kai, feb'17
	private static final Logger LOG = Logger.getLogger(BicycleLegScoring.class);


	protected double score;

	/** The parameters used for scoring */
	protected final ScoringParameters params;
	protected Network network;
	private boolean nextEnterVehicleIsFirstOfTrip = true ;
	private boolean nextStartPtLegIsFirstOfTrip = true ;
	private boolean currentLegIsPtLeg = false;
	private double lastActivityEndTime = Time.UNDEFINED_TIME ;
	
	//
	private final double marginalUtilityOfInfrastructure_m;
	private final double marginalUtilityOfComfort_m;
	private final double marginalUtilityOfGradient_m_100m;
	//
	
//	public BicycleLegScoring(final ScoringParameters params, Network network) {
	public BicycleLegScoring(final ScoringParameters params, Network network, BicycleConfigGroup bicycleConfigGroup) {
		this.params = params;
		this.network = network;
		this.nextEnterVehicleIsFirstOfTrip = true ;
		this.nextStartPtLegIsFirstOfTrip = true ;
		this.currentLegIsPtLeg = false;
		
		//
		this.marginalUtilityOfInfrastructure_m = bicycleConfigGroup.getMarginalUtilityOfInfrastructure_m();
		this.marginalUtilityOfComfort_m = bicycleConfigGroup.getMarginalUtilityOfComfort_m();
		this.marginalUtilityOfGradient_m_100m = bicycleConfigGroup.getMarginalUtilityOfGradient_m_100m();
		//
	}

	@Override
	public void finish() {

	}

	@Override
	public double getScore() {
		return this.score;
	}

	private static int ccc=0 ;
	
	protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // travel time in seconds	
		ModeUtilityParameters modeParams = this.params.modeParams.get(leg.getMode());
		if (modeParams == null) {
			if (leg.getMode().equals(TransportMode.transit_walk) || leg.getMode().equals(TransportMode.access_walk) 
					|| leg.getMode().equals(TransportMode.egress_walk) ) {
				modeParams = this.params.modeParams.get(TransportMode.walk);
			} else {
//				modeParams = this.params.modeParams.get(TransportMode.other);
				throw new RuntimeException("just encountered mode for which no scoring parameters are defined: " + leg.getMode().toString() ) ;
			}
		}
		tmpScore += travelTime * modeParams.marginalUtilityOfTraveling_s;
		if (modeParams.marginalUtilityOfDistance_m != 0.0
				|| modeParams.monetaryDistanceCostRate != 0.0) {
			Route route = leg.getRoute();
			double dist = route.getDistance(); // distance in meters
			if ( Double.isNaN(dist) ) {
				if ( ccc<10 ) {
					ccc++ ;
					Logger.getLogger(this.getClass()).warn("distance is NaN. Will make score of this plan NaN. Possible reason: Simulation does not report " +
							"a distance for this trip. Possible reason for that: mode is teleported and router does not " +
							"write distance into plan.  Needs to be fixed or these plans will die out.") ;
					if ( ccc==10 ) {
						Logger.getLogger(this.getClass()).warn(Gbl.FUTURE_SUPPRESSED) ;
					}
				}
			}
			tmpScore += modeParams.marginalUtilityOfDistance_m * dist;
			tmpScore += modeParams.monetaryDistanceCostRate * this.params.marginalUtilityOfMoney * dist;
		}
		//
		tmpScore += addBicycleScoringComponent(leg.getRoute());
		tmpScore += modeParams.constant;
		// (yyyy once we have multiple legs without "real" activities in between, this will produce wrong results.  kai, dec'12)
		// (yy NOTE: the constant is added for _every_ pt leg.  This is not how such models are estimated.  kai, nov'12)
		return tmpScore;
	}
	
	
	private double addBicycleScoringComponent(Route route) {
		NetworkRoute networkRoute = (NetworkRoute) route;
		List<Id<Link>> linkIds = new ArrayList<>();
		linkIds.addAll(networkRoute.getLinkIds());
		linkIds.add(networkRoute.getEndLinkId());
		
		double score = 0.;
		
		for (Id<Link> linkId : linkIds) {
			Link link = network.getLinks().get(linkId);
			double scoreOnLink = getTravelDisutilityBasedOnTTime(link);
			LOG.info("Bicycle score on link " + linkId + " is = " + scoreOnLink + ".");
			score += scoreOnLink;
		}
		LOG.info("Bicycle leg score component is = " + score + " (on links " + linkIds + ").");
		return score;
	}
	
	

	public double getTravelDisutilityBasedOnTTime(Link link) {
		String surface = (String) link.getAttributes().getAttribute(BicycleLabels.SURFACE);
		String type = (String) link.getAttributes().getAttribute("type");
		String cyclewaytype = (String) link.getAttributes().getAttribute(BicycleLabels.CYCLEWAY);

		double distance = link.getLength();
		
//		double travelTimeDisutility = marginalCostOfTime_s * travelTime;
//		double distanceDisutility = marginalCostOfDistance_m * distance;
		
		double comfortFactor = getComfortFactor(surface, type);
		double comfortDisutility = marginalUtilityOfComfort_m * (1. - comfortFactor) * distance;
		
		double infrastructureFactor = getInfrastructureFactor(type, cyclewaytype);
		double infrastructureDisutility = marginalUtilityOfInfrastructure_m * (1. - infrastructureFactor) * distance;
		
		double gradientFactor = getGradientFactor(link);
		double gradientDisutility = marginalUtilityOfGradient_m_100m * gradientFactor * distance;
		
//		LOG.warn("link = " + link.getId() + "-- travelTime = " + travelTime + " -- distance = " + distance + " -- comfortFactor = "
//				+ comfortFactor	+ " -- infraFactor = "+ infrastructureFactor + " -- gradient = " + gradientFactor);
		 
		// TODO Gender
		// TODO Activity
		// TODO Other influence factors

//		 double normalization = 1;
//		 if (sigma != 0.) {
//			 normalization = 1. / Math.exp(this.sigma * this.sigma / 2);
//			 if (normalisationWrnCnt < 10) {
//				 normalisationWrnCnt++;
//				 LOG.info("Sigma = " + this.sigma + " -- resulting normalization: " + normalization);
//			 }
//		}
//		Random random2 = MatsimRandom.getLocalInstance(); // Make sure that stream of random variables is reproducible. dz, aug'17
//		double logNormalRnd = Math.exp(sigma * random2.nextGaussian());
//		logNormalRnd *= normalization;

//		LOG.warn("link = " + link.getId() + " -- travelTimeDisutility = " + travelTimeDisutility + " -- distanceDisutility = "+ distanceDisutility
//				+ " -- infrastructureDisutility = " + infrastructureDisutility + " -- comfortDisutility = "
//				+ comfortDisutility + " -- gradientDisutility = " + gradientDisutility + " -- randomfactor = " + logNormalRnd);
		return (infrastructureDisutility + comfortDisutility + gradientDisutility);
//		return (travelTimeDisutility + logNormalRnd * (distanceDisutility + infrastructureDisutility + comfortDisutility + gradientDisutility));
	}

	
	
	private double getGradientFactor(Link link) {
		double gradient = 0.;
		Double fromNodeZ = link.getFromNode().getCoord().getZ();
		Double toNodeZ = link.getToNode().getCoord().getZ();
		if ((fromNodeZ != null) && (toNodeZ != null)) {
			if (toNodeZ > fromNodeZ) { // No positive utility for downhill, only negative for uphill
				gradient = (toNodeZ - fromNodeZ) / link.getLength();
			}
		}
		return gradient;
	}

	// TODO combine this with speeds
	private double getComfortFactor(String surface, String type) {
		double comfortFactor = 1.0;
		if (surface != null) {
			switch (surface) {
			case "paved":
			case "asphalt": comfortFactor = 1.0; break;
			case "cobblestone": comfortFactor = .40; break;
			case "cobblestone (bad)": comfortFactor = .30; break;
			case "sett": comfortFactor = .50; break;
			case "cobblestone;flattened":
			case "cobblestone:flattened": comfortFactor = .50; break;
			case "concrete": comfortFactor = .100; break;
			case "concrete:lanes": comfortFactor = .95; break;
			case "concrete_plates":
			case "concrete:plates": comfortFactor = .90; break;
			case "paving_stones": comfortFactor = .80; break;
			case "paving_stones:35":
			case "paving_stones:30": comfortFactor = .80; break;
			case "unpaved": comfortFactor = .60; break;
			case "compacted": comfortFactor = .70; break;
			case "dirt":
			case "earth": comfortFactor = .30; break;
			case "fine_gravel": comfortFactor = .90; break;
			case "gravel":
			case "ground": comfortFactor = .60; break;
			case "wood":
			case "pebblestone":
			case "sand": comfortFactor = .30; break;
			case "bricks": comfortFactor = .60; break;
			case "stone":
			case "grass":
			case "compressed": comfortFactor = .40; break;
			case "asphalt;paving_stones:35": comfortFactor = .60; break;
			case "paving_stones:3": comfortFactor = .40; break;
			default: comfortFactor = .85;
			}
		} else {
			// For many primary and secondary roads, no surface is specified because they are by default assumed to be is asphalt.
			// For tertiary roads street this is not true, e.g. Friesenstr. in Kreuzberg
			if (type != null) {
				if (type.equals("primary") || type.equals("primary_link") || type.equals("secondary") || type.equals("secondary_link")) {
					comfortFactor = 1.0;
				}
			}
		}
		return comfortFactor;
	}

	private double getInfrastructureFactor(String type, String cyclewaytype) {
		double infrastructureFactor = 1.0;
		if (type != null) {
			if (type.equals("trunk")) {
				if (cyclewaytype == null || cyclewaytype.equals("no") || cyclewaytype.equals("none")) { // No cycleway
					infrastructureFactor = .05;
				} else { // Some kind of cycleway
					infrastructureFactor = .95;
				}
			} else if (type.equals("primary") || type.equals("primary_link")) {
				if (cyclewaytype == null || cyclewaytype.equals("no") || cyclewaytype.equals("none")) { // No cycleway
					infrastructureFactor = .10;
				} else { // Some kind of cycleway
					infrastructureFactor = .95;
				}
			} else if (type.equals("secondary") || type.equals("secondary_link")) {
				if (cyclewaytype == null || cyclewaytype.equals("no") || cyclewaytype.equals("none")) { // No cycleway
					infrastructureFactor = .30;
				} else { // Some kind of cycleway
					infrastructureFactor = .95;
				}
			} else if (type.equals("tertiary") || type.equals("tertiary_link")) {
				if (cyclewaytype == null || cyclewaytype.equals("no") || cyclewaytype.equals("none")) { // No cycleway
					infrastructureFactor = .40;
				} else { // Some kind of cycleway
					infrastructureFactor = .95;
				}
			} else if (type.equals("unclassified")) {
				if (cyclewaytype == null || cyclewaytype.equals("no") || cyclewaytype.equals("none")) { // No cycleway
					infrastructureFactor = .90;
				} else { // Some kind of cycleway
					infrastructureFactor = .95;
				}
			} else if (type.equals("unclassified")) {
				infrastructureFactor = .95;
			} else if (type.equals("service") || type.equals("living_street") || type.equals("minor")) {
				infrastructureFactor = .95;
			} else if (type.equals("cycleway") || type.equals("path")) {
				infrastructureFactor = 1.00;
			} else if (type.equals("footway") || type.equals("track") || type.equals("pedestrian")) {
				infrastructureFactor = .95;
			} else if (type.equals("steps")) {
				infrastructureFactor = .10;
			}
		} else {
			infrastructureFactor = .85;
		}
		return infrastructureFactor;
	}
	
	@Override
	public void handleEvent(Event event) {
		if ( event instanceof ActivityEndEvent ) {
			// When there is a "real" activity, flags are reset:
			if ( !PtConstants.TRANSIT_ACTIVITY_TYPE.equals( ((ActivityEndEvent)event).getActType()) ) {
				this.nextEnterVehicleIsFirstOfTrip  = true ;
				this.nextStartPtLegIsFirstOfTrip = true ;
			}
			this.lastActivityEndTime = event.getTime() ;
		}

		if ( event instanceof PersonEntersVehicleEvent && currentLegIsPtLeg ) {
			if ( !this.nextEnterVehicleIsFirstOfTrip ) {
				// all vehicle entering after the first triggers the disutility of line switch:
				this.score  += params.utilityOfLineSwitch ;
			}
			this.nextEnterVehicleIsFirstOfTrip = false ;
			// add score of waiting, _minus_ score of travelling (since it is added in the legscoring above):
			this.score += (event.getTime() - this.lastActivityEndTime) * (this.params.marginalUtilityOfWaitingPt_s - this.params.modeParams.get(TransportMode.pt).marginalUtilityOfTraveling_s) ;
		}

		if ( event instanceof PersonDepartureEvent ) {
			this.currentLegIsPtLeg = TransportMode.pt.equals( ((PersonDepartureEvent)event).getLegMode() );
			if ( currentLegIsPtLeg ) {
				if ( !this.nextStartPtLegIsFirstOfTrip ) {
					this.score -= params.modeParams.get(TransportMode.pt).constant ;
					// (yyyy deducting this again, since is it wrongly added above.  should be consolidated; this is so the code
					// modification is minimally invasive.  kai, dec'12)
				}
				this.nextStartPtLegIsFirstOfTrip = false ;
			}
		}
	}

	@Override
	public void handleLeg(Leg leg) {
		double legScore = calcLegScore(leg.getDepartureTime(), leg.getDepartureTime() + leg.getTravelTime(), leg);
		this.score += legScore;
	}


}
