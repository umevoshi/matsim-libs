package playground.kai.bvwp;

import org.matsim.core.basic.v01.IdImpl;

import playground.kai.bvwp.Values.Attribute;
import playground.kai.bvwp.Values.Mode;
import playground.kai.bvwp.Values.DemandSegment;

class ScenarioForTest1 {

	static ScenarioForEvalData createNullfallForTest() {
		// set up the base case:
		ScenarioForEvalData nullfall = new ScenarioForEvalData() ;
	
		// construct values for one OD relation:
		Values nullfallForOD = new Values() ;
		nullfall.setValuesForODRelation(new IdImpl("AB"), nullfallForOD ) ;
		{
			// construct values for the road mode for this OD relation:
			ValuesForAMode roadValues = nullfallForOD.getByMode(Mode.road) ;
			{
				// passenger traffic:
				ValuesForAUserType pvValues = roadValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL) ;
				pvValues.setByEntry( Attribute.XX, 1000. ) ; // number of persons
				pvValues.setByEntry( Attribute.km, 10. ) ;
				pvValues.setByEntry( Attribute.hrs, 1. ) ;
			}
			{
				// freight traffic:
				ValuesForAUserType gvValues = roadValues.getByDemandSegment(DemandSegment.GV) ;
				gvValues.setByEntry( Attribute.XX, 1000. ) ; // tons
				gvValues.setByEntry( Attribute.km, 10. ) ;
				gvValues.setByEntry( Attribute.hrs, 1. ) ;
			}				
			
			// rail values are just a copy of the road values:
			ValuesForAMode railValues = roadValues.createDeepCopy() ;
			nullfallForOD.setValuesForMode( Mode.rail, railValues ) ;
		}
		
		// return the base case:
		return nullfall;
	}

	static ScenarioForEvalData createPlanfallForTest(ScenarioForEvalData nullfall) {
		// (construct the policy case.  The base case can be used to simplify things ...)
		
		// The policy case is initialized as a complete copy of the base case:
		ScenarioForEvalData planfall = nullfall.createDeepCopy() ;
		
		// we are now looking at one specific OD relation (for this scenario, there is only one!)
		Values planfallForOD = planfall.getByODRelation(new IdImpl("AB")) ;
		{
			// modify the travel times for the rail mode:
			ValuesForAMode railValues = planfallForOD.getByMode( Mode.rail ) ;
			railValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL).incByEntry( Attribute.hrs, -0.1 ) ;
			railValues.getByDemandSegment(DemandSegment.GV).incByEntry( Attribute.hrs, -0.1 ) ;
			
			// modify some demand (presumably as a result):
			double delta = 100. ;
			railValues.getByDemandSegment(DemandSegment.GV).incByEntry( Attribute.XX, delta ) ;
			planfall.getByODRelation(new IdImpl("AB")).getByMode(Mode.road).getByDemandSegment(DemandSegment.GV).incByEntry(Attribute.XX, -delta ) ;
		}
		return planfall;
	}

}
