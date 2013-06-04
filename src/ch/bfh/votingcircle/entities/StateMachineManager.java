package ch.bfh.votingcircle.entities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import ch.bfh.votingcircle.statemachine.actions.CommitmentRoundAction;
import ch.bfh.votingcircle.statemachine.actions.ResultAction;
import ch.bfh.votingcircle.statemachine.actions.InitAction;
import ch.bfh.votingcircle.statemachine.actions.RecoveryRoundAction;
import ch.bfh.votingcircle.statemachine.actions.SetupRoundAction;
import ch.bfh.votingcircle.statemachine.actions.StartAction;
import ch.bfh.votingcircle.statemachine.actions.TallyingAction;
import ch.bfh.votingcircle.statemachine.actions.VotingMessage;
import ch.bfh.votingcircle.statemachine.actions.VotingRoundAction;
import ch.bfh.votingcircle.statemachine.events.AllCommitMessagesReceivedEvent;
import ch.bfh.votingcircle.statemachine.events.AllInitMessagesReceivedEvent;
import ch.bfh.votingcircle.statemachine.events.AllRecoveringMessagesReceivedEvent;
import ch.bfh.votingcircle.statemachine.events.AllSetupMessagesReceivedEvent;
import ch.bfh.votingcircle.statemachine.events.AllVotingMessagesReceivedEvent;
import ch.bfh.votingcircle.statemachine.events.NotAllMessageReceivedEvent;
import ch.bfh.votingcircle.statemachine.events.ResultComputedEvent;
import ch.bfh.votingcircle.statemachine.events.StartCycleEvent;
import ch.bfh.votingcircle.statemachine.events.StartProtocolEvent;

import com.continuent.tungsten.fsm.core.EntityAdapter;
import com.continuent.tungsten.fsm.core.Event;
import com.continuent.tungsten.fsm.core.EventTypeGuard;
import com.continuent.tungsten.fsm.core.FiniteStateException;
import com.continuent.tungsten.fsm.core.Guard;
import com.continuent.tungsten.fsm.core.State;
import com.continuent.tungsten.fsm.core.StateMachine;
import com.continuent.tungsten.fsm.core.StateTransitionMap;
import com.continuent.tungsten.fsm.core.StateType;

/**
 * Creation of the State Machine managing the flow of the protocol
 * @author Philémon von Bergen
 *
 */
public class StateMachineManager implements Runnable {

	private StateMachine sm;
	private DataManager dataManager;
	private StartAction startAction;
	private InitAction initAction;
	private SetupRoundAction setupRoundAction;
	private CommitmentRoundAction commitmentRoundAction;
	private VotingRoundAction votingRoundAction;
	private TallyingAction tallyingAction;
	private RecoveryRoundAction recoveryRoundAction;

	/**
	 * Create an object managing the state machine
	 * @param dataManager
	 */
	public StateMachineManager(DataManager dataManager) {
		this.dataManager = dataManager;
		LocalBroadcastManager.getInstance(dataManager.getContext()).registerReceiver(applyTransition, new IntentFilter("applyTransition"));
	}

	/**
	 * Create and run the state machine
	 */
	@Override
	public void run() {

		/*Create the state machine*/
		StateTransitionMap stmap = new StateTransitionMap();
		
		/*Define actions*/
		startAction = new StartAction(VotingMessage.MSG_CONTENT_START,this.dataManager);
		initAction = new InitAction(VotingMessage.MSG_CONTENT_INIT,this.dataManager);
		setupRoundAction = new SetupRoundAction(VotingMessage.MSG_CONTENT_SETUP,this.dataManager);
		commitmentRoundAction = new CommitmentRoundAction(VotingMessage.MSG_CONTENT_COMMIT,this.dataManager);
		votingRoundAction = new VotingRoundAction(VotingMessage.MSG_CONTENT_VOTE,this.dataManager);
		tallyingAction = new TallyingAction(VotingMessage.MSG_CONTENT_TALLY,this.dataManager);
		recoveryRoundAction = new RecoveryRoundAction(VotingMessage.MSG_CONTENT_RECOVER,this.dataManager);

		/*Define states*/
		State begin = new State("begin", StateType.START, null, null);
		State start = new State("start", StateType.ACTIVE, startAction, null);
		State init = new State("init", StateType.ACTIVE, initAction, null);
		State setup = new State("setup", StateType.ACTIVE, setupRoundAction, null);
		State commit = new State("commit", StateType.ACTIVE, commitmentRoundAction, null);
		State vote = new State("vote", StateType.ACTIVE, votingRoundAction, null);
		State tally = new State("tally", StateType.ACTIVE, tallyingAction, null);
		State recovery = new State("recover", StateType.ACTIVE, recoveryRoundAction, null);
		State exit = new State("exit", StateType.END, new ResultAction(this.dataManager), null);

		
		/*Define Guards (=conditions) for transitions*/
		Guard startCycle = new EventTypeGuard(StartCycleEvent.class);
		Guard startProtocol = new EventTypeGuard(StartProtocolEvent.class);
		Guard allInitMessagesReceived = new EventTypeGuard(AllInitMessagesReceivedEvent.class);
		Guard allSetupMessagesReceived = new EventTypeGuard(AllSetupMessagesReceivedEvent.class);
		Guard allCommitMessagesReceived = new EventTypeGuard(AllCommitMessagesReceivedEvent.class);
		Guard allVotingMessagesReceived = new EventTypeGuard(AllVotingMessagesReceivedEvent.class);
		Guard allRecoveringMessagesReceived = new EventTypeGuard(AllRecoveringMessagesReceivedEvent.class);
		Guard notAllMessageReceived = new EventTypeGuard(NotAllMessageReceivedEvent.class);
		Guard resultComputed = new EventTypeGuard(ResultComputedEvent.class);
		
		try {
			/*Add states*/
			stmap.addState(begin);
			stmap.addState(start);
			stmap.addState(init);
			stmap.addState(setup);
			stmap.addState(commit);
			stmap.addState(vote);
			stmap.addState(tally);
			stmap.addState(recovery);			
			stmap.addState(exit);
		
			/*Add transitions*/

			//Transition of state begin
			stmap.addTransition("begin-start", startCycle, begin, null, start);
			//Transition of state start
			stmap.addTransition("start-init", startProtocol, start, null, init);
			//Transition of state init
			stmap.addTransition("init-setup", allInitMessagesReceived, init, null, setup);
			//Transition of state setup
			stmap.addTransition("setup-commit", allSetupMessagesReceived, setup, null, commit);
			//Transition of state commit
			stmap.addTransition("commit-vote", allCommitMessagesReceived, commit, null, vote);
			//Transition of state vote
			stmap.addTransition("vote-tally", allVotingMessagesReceived, vote, null, tally);
			//Transition of state tally
			stmap.addTransition("tally-exit", resultComputed, tally, null, exit);
			//Transition of state recovery
			stmap.addTransition("tally-recovery", notAllMessageReceived, tally, null, recovery);
			stmap.addTransition("recovery-tally", allRecoveringMessagesReceived, recovery, null, tally);
		
			/*Build map*/
			stmap.build();

		} catch (FiniteStateException e) {
			DataManager.LOGGER.debug(e);
		}

		/*Start state machine*/
		DataManager.LOGGER.debug("Starting state machine");
		sm = new StateMachine(stmap, new EntityAdapter(this));

	}
	
	/**
	 * Returns the state machine
	 * @return
	 */
	public StateMachine getStateMachine(){
		return sm;
	}
	
	/**
	 * Reset the states of the state machine
	 */
	public void reset(){
		this.startAction.reset();
		this.initAction.reset();
		this.setupRoundAction.reset();
		this.commitmentRoundAction.reset();
		this.votingRoundAction.reset();
		this.tallyingAction.reset();
		this.recoveryRoundAction.reset();
	}
	
	/**
	 * Listen for broadcasts asking to apply a transition
	 */
	private BroadcastReceiver applyTransition = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Event event = (Event)intent.getSerializableExtra("event");
			try {
				sm.applyEvent(event);
			} catch (FiniteStateException e) {
				DataManager.LOGGER.debug(e);
			} catch (InterruptedException e) {
				DataManager.LOGGER.debug(e);
			}
		}
	};
	
}
