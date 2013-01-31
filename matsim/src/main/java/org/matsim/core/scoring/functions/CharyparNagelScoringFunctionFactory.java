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

package org.matsim.core.scoring.functions;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;

/**
 * A factory to create scoring functions as described by D. Charypar and K. Nagel.
 * 
 * <blockquote>
 *  <p>Charypar, D. und K. Nagel (2005) <br>
 *  Generating complete all-day activity plans with genetic algorithms,<br>
 *  Transportation, 32 (4) 369-397.</p>
 * </blockquote>
 * 
 * @author rashid_waraich
 */
public class CharyparNagelScoringFunctionFactory implements ScoringFunctionFactory {

//	private final CharyparNagelScoringParameters params;
	protected Network network;
	private final PlanCalcScoreConfigGroup config;

	public CharyparNagelScoringFunctionFactory(final PlanCalcScoreConfigGroup config, Network network) {

		//		this.params = new CharyparNagelScoringParameters(config);
		this.config = config ;
		// (constructor of factory should not "do" anything.  In this case, config may not yet contain the
		// "pt interaction" activity type when this constructor is called.  kai, jan'13)
		
		this.network = network;
	}

//	public CharyparNagelScoringFunctionFactory(CharyparNagelScoringParameters params, Network network) {
//		this.params = params;
//		this.network = network;
//	}


	/**
	 * puts the scoring functions together, which form the
	 * CharyparScoringFunction
	 * <p/>
	 * This creational method gets the plan as an argument.  Since it is possible to get the person from the plan, it is thus
	 * possible to make the scoring function person-specific.
	 * <p/>  
	 * Notes:<ul>
	 * <li>If I understand this correctly, this creational method is 
	 * called in every iteration. kai, apr'11
	 * <li>The fact that you have a person-specific scoring function does not mean that the "creative" modules
	 * (such as route choice) are person-specific.  This is not a bug but a deliberate design concept in order 
	 * to reduce the consistency burden.  Instead, the creative modules should generate a diversity of possible
	 * solutions.  In order to do a better job, they may (or may not) use person-specific info.  kai, apr'11
	 * </ul>
	 * 
	 * @param plan
	 * @return new ScoringFunction
	 */
	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(this.config) ;
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, network));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(params));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
		return scoringFunctionAccumulator;
	}

	@Deprecated
	public CharyparNagelScoringParameters getParams() {
		// yyyy This is not helpful.  If factories should not do anything before "create" is called, then it is not clear
		// in which state this is at which point in time.  (For example, the "pt interaction" activity may have already been added,
		//	 or not.not kai, jan'13
//		return params;
		return new CharyparNagelScoringParameters(config) ;
	}
}
