package ch.bfh.votingcircle.statemachine.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import ch.bfh.instacircle.Message;
import ch.bfh.instacircle.R;
import ch.bfh.votingcircle.EvotingMainActivity;
import ch.bfh.votingcircle.entities.DataManager;
import ch.bfh.votingcircle.statemachine.events.StartProtocolEvent;

import com.continuent.tungsten.fsm.core.Entity;
import com.continuent.tungsten.fsm.core.Event;

import com.continuent.tungsten.fsm.core.Transition;
import com.continuent.tungsten.fsm.core.TransitionFailureException;
import com.continuent.tungsten.fsm.core.TransitionRollbackException;


/**
 * Action executed in Start step
 * This action wait a start message coming from the initiator of the protocol
 * @author Philémon von Bergen
 * 
 */
public class StartAction extends AbstractAction {

	private Timer timerBeacon;
	private TaskTimer timerTask = new TaskTimer();
	private boolean timerBeaconStarted = false;

	/**
	 * Create the object
	 * @param messageTypeToListenTo the type of message to listen to at this step
	 */
	public StartAction(int messageTypeToListenTo, DataManager dm){
		super(messageTypeToListenTo, dm);
		this.classname = this.getClass().getSimpleName(); 
	}

	@Override
	public void doAction(Event message, Entity entity, Transition transition,
			int actionType) throws TransitionRollbackException,
			TransitionFailureException {
		
		logger.debug("StartAction started");

		//Listen to participant leaved events
		IntentFilter intentFilter = new IntentFilter("participantChangedState");
		LocalBroadcastManager.getInstance(dm.getContext()).registerReceiver(participantsLeft, intentFilter);

		if(!timerBeaconStarted){
			timerBeacon = new Timer();
			timerBeacon.schedule(timerTask, 5000, 5000);
			timerBeaconStarted=true;
		}

		this.actionRan = true;

		//Wait for receiving values from other participants
		if(this.readyToGoToNextState()){
			this.goToNextState();
		} else {
			Intent intentToSend = new Intent("countParticipants");
			LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intentToSend);
			startTimer(20000, Integer.MAX_VALUE);
		}

	}

	@Override
	protected void processMessage(Message message){
		if(this.readyToGoToNextState()){
			this.goToNextState();
		}
	}

	@Override
	protected boolean readyToGoToNextState(){
		if(!this.actionRan) return false;
		if(this.messagesReceived.size()==1){
			return true;
		} else if(this.messagesReceived.size()>1){
			throw new RuntimeException("To much start message received");
		} else {
			return false;
		}
	}

	@Override
	protected void goToNextState(){

		if(!dm.isActive()) return;
		EvotingMainActivity activity = this.dm.getEvotingMainActivity();
		
		this.dm.setProtocolStarted(true);

		//Remember who initiated the voting protocol
		List<Message> list = new ArrayList<Message>(this.messagesReceived.values());
		this.dm.setInitiator(list.get(0).getSenderIPAddress());
		logger.debug(this.classname +" Initiator of the voting protocol is: "+this.dm.getInitiator());

		//Close config activity and remove config button
		if(this.dm.getElectionInfoActivity()!=null) this.dm.getElectionInfoActivity().finish();
		
		activity.showProgressDialog(activity.getString(R.string.progress_dialog_title), activity.getString(R.string.progress_dialog_text));

		Button b = (Button)activity.findViewById(R.id.config_button);
		ViewGroup vg = (ViewGroup)b.getParent();
		vg.removeView(b);

		View v1 = (View) activity.findViewById(R.id.evoting_main_view_border_1);
		vg.removeView(v1);

		View v2 = (View) activity.findViewById(R.id.evoting_main_view_space_1);
		vg.removeView(v2);

		View v3 = (View) activity.findViewById(R.id.evoting_main_view_space_2);
		vg.removeView(v3);

		Button b2 = (Button)activity.findViewById(R.id.start_button);
		b2.setVisibility(View.GONE);

		View v6 = (View) activity.findViewById(R.id.evoting_main_view_border_2);
		v6.setVisibility(View.GONE);

		View v4 = (View) activity.findViewById(R.id.evoting_main_view_space_3);
		v4.setVisibility(View.GONE);

		View v5 = (View) activity.findViewById(R.id.evoting_main_view_space_4);
		v5.setVisibility(View.GONE);

		this.actionTerminated = true;
	
		this.reset();

		//Ask for applying transition
		Intent transitionIntent = new Intent("applyTransition");
		transitionIntent.putExtra("event", new StartProtocolEvent(null));
		LocalBroadcastManager.getInstance(this.dm.getContext()).sendBroadcast(transitionIntent);
	}
	
	@Override
	public void reset(){
		super.reset();
		timerTask.cancel();
		timerBeacon.cancel();
		timerBeacon.purge();
		timerBeaconStarted=false;
		
	}

	/**
	 * Task run on timer tick
	 * 
	 */
	private class TaskTimer extends TimerTask {

		@Override
		public void run() {
			if(dm !=null && dm.isActive()){
				sendMessage("Beacon from "+dm.getMyIdentification(),VotingMessage.MSG_CONTENT, null);
				logger.debug("Sending beacon");
				Intent intent = new Intent("countParticipants");
				LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intent);
			}
		}
	}
}
