package opdytsintegration;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import optdyts.DecisionVariable;
import optdyts.ObjectiveFunction;
import optdyts.SimulatorState;
import optdyts.algorithms.DecisionVariableSetEvaluator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.math.Vector;

/**
 * Identifies the approximately best out of a set of decision variables.
 * 
 * @author Gunnar Flötteröd
 *
 * @param <X>
 *            the simulator state type
 * @param <U>
 *            the decision variable type
 */
public class MATSimDecisionVariableSetEvaluator<X extends SimulatorState<X>, U extends DecisionVariable>
		implements StartupListener, BeforeMobsimListener {

	// -------------------- MEMBERS --------------------

	// CONSTANTS

	private final DecisionVariableSetEvaluator<X, U> evaluator;

	private final MATSimStateFactory<X, U> stateFactory;

	// RUNTIME VARIABLES

	private boolean initialIteration = true;

	private SortedSet<Id<Link>> sortedLinkIds = null;

	private DynamicData<Id<Link>> data = null;

	// -------------------- CONSTRUCTION --------------------

	public MATSimDecisionVariableSetEvaluator(final Set<U> decisionVariables,
			final ObjectiveFunction<X> objectiveFunction,
			final double simulatedNoiseVariance, final double maxGap2,
			final MATSimStateFactory<X, U> stateFactory) {
		this.evaluator = new DecisionVariableSetEvaluator<X, U>(
				decisionVariables, objectiveFunction, simulatedNoiseVariance,
				maxGap2);
		this.stateFactory = stateFactory;
	}

	// --------------- CONTROLLER LISTENER IMPLEMENTATIONS ---------------

	@Override
	public void notifyStartup(final StartupEvent event) {

		this.evaluator.implementNextDecisionVariable();

		this.data = new DynamicData<Id<Link>>(0, 3600, 24);
		this.sortedLinkIds = new TreeSet<Id<Link>>(event.getControler()
				.getScenario().getNetwork().getLinks().keySet());
		event.getControler().getEvents()
				.addHandler(new LinkEnterEventHandler() {
					@Override
					public void reset(final int iteration) {
					}

					@Override
					public void handleEvent(final LinkEnterEvent event) {
						final int bin = Math.min(
								data.bin((int) event.getTime()),
								data.getBinCnt() - 1);
						data.add(event.getLinkId(), bin, 1.0);
					}
				});
	}

	@Override
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {

		if (this.initialIteration) {
			this.initialIteration = false;
		} else {
			
			final Vector newStateVector = new Vector(this.sortedLinkIds.size()
					* this.data.getBinCnt());
			int i = 0;
			for (Id<Link> id : this.sortedLinkIds) {
				for (int bin = 0; bin < this.data.getBinCnt(); bin++) {
					newStateVector.set(i++, this.data.getBinValue(id, bin));
				}
			}

			final X newState = this.stateFactory.newState(event.getControler()
					.getScenario().getPopulation(), newStateVector,
					this.evaluator.getCurrentDecisionVariable());
			this.evaluator.registerState(newState);

			this.data.clear();
			this.evaluator.implementNextDecisionVariable();
		}
	}
}
