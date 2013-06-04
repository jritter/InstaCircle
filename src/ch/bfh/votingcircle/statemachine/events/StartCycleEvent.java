package ch.bfh.votingcircle.statemachine.events;

import java.io.Serializable;

import com.continuent.tungsten.fsm.core.Event;

/**
 * State machnine event used for transition to state strat state
 * @author Philémon von Bergen
 *
 */
public class StartCycleEvent extends Event implements Serializable{

	private static final long serialVersionUID = 1L;

	public StartCycleEvent(Object data) {
		super(data);
	}
}
