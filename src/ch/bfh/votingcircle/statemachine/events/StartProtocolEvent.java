package ch.bfh.votingcircle.statemachine.events;

import java.io.Serializable;

import com.continuent.tungsten.fsm.core.Event;

/**
 * State machnine event used for transition from state start to state init
 * @author Philémon von Bergen
 *
 */
public class StartProtocolEvent extends Event implements Serializable{

	private static final long serialVersionUID = 1L;

	public StartProtocolEvent(Object data) {
		super(data);
	}
}
