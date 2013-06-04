package ch.bfh.votingcircle.statemachine.actions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import ch.bfh.instacircle.Message;
import ch.bfh.votingcircle.entities.DataManager;
import ch.bfh.votingcircle.entities.Participant;

import com.continuent.tungsten.fsm.core.Action;
import com.continuent.tungsten.fsm.core.Entity;
import com.continuent.tungsten.fsm.core.Event;
import com.continuent.tungsten.fsm.core.Transition;
import com.continuent.tungsten.fsm.core.TransitionFailureException;
import com.continuent.tungsten.fsm.core.TransitionRollbackException;

/**
 * Abstract class representing the action done at a step of the protocol
 * This class implement the logic of receiving and sending a message
 * @author Philémon von Bergen
 *
 */
public abstract class AbstractAction implements Action {

	protected static final String RESEND_REQUEST = "Please resend";

	private int messageTypeToListenTo = 1;
	protected String classname = "";

	protected Map<String,Message> messagesReceived;

	protected DataManager dm;
	protected Logger logger;

	private boolean timerIsRunning = false;
	private Timer timer;
	private TaskTimer timerTask;

	protected int numberOfTimerRuns = 0;

	protected boolean actionRan = false;
	protected boolean actionTerminated = false;

	/**
	 * Create an  Action object
	 * 
	 * @param messageTypeToListenTo the type of message that concern this action
	 * @param dm DataManager object
	 */
	public AbstractAction(int messageTypeToListenTo, DataManager dm) {

		this.messageTypeToListenTo = messageTypeToListenTo;

		//Map of message received
		this.messagesReceived = new HashMap<String,Message>();

		//Store some important entities that will be used in the actions
		this.dm = dm;
		this.logger = DataManager.LOGGER;


		// Subscribing to the messageArrived events to update immediately
		LocalBroadcastManager.getInstance(dm.getContext()).registerReceiver(
				this.mMessageReceiver, new IntentFilter("messageArrived"));
		
	}
	
	/**
	 * Method containing stuff to do in the current state 
	 */
	@Override
	public void doAction(Event arg0, Entity arg1, Transition arg2, int arg3)
			throws TransitionRollbackException, TransitionFailureException,
			InterruptedException {
		throw new UnsupportedOperationException("Method must be overriden");
	}


	/**
	 * Store the received messages if they are from the interesting type for this step 
	 */
	protected BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			
			if(!dm.isActive()) return;

			//Get the message
			Bundle messages = intent.getExtras();
			Message message = (Message) messages.get("message");

			
			//If it is a message we are interested in (resend request are not considered as interesting, ResendManager cares about them)
			if (message.getMessageType() == messageTypeToListenTo) {
				if(!messagesReceived.containsKey(message.getSenderIPAddress())){
					messagesReceived.put(message.getSenderIPAddress(), message);
					logger.debug(classname +" Message received");
				}
				else if (!messagesReceived.get(message.getSenderIPAddress()).equals(message) && message.getMessageType()!=VotingMessage.MSG_CONTENT_INIT){
					//when resending init message, state of some participant could have changed, so message can differ from one received previously
					logger.warn("Seems to receive a different message from same source !!! \nOriginal message "+ messagesReceived.get(message.getSenderIPAddress()).getMessage() + "\nNew message "+ message.getMessage());
				}
				
				//When a new message arrives, we re-execute the action of the step
				if(actionTerminated){
					logger.warn(classname +" Action was called by an incoming message, but was already terminated");
					return;
				}
				processMessage(message);

			}
		}
	};
	
	/**
	 * if the state of a participant has been
	 * changed and she has leaved the discussion, we can put her in the excluded participants
	 */
	protected BroadcastReceiver participantsLeaved = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(!dm.isActive()) return;
			
			//Get the message
			String ip = intent.getStringExtra("participant");
			//Exclude participant
			//could be null in start action and init action
			if(dm.getProtocolParticipants()!=null){
				Participant p = dm.getProtocolParticipants().remove(ip);
				//if participant not already excluded
				if(p!=null){
					p.setState(-1);
					dm.getExcludedParticipants().add(p);
					LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(new Intent("participantMessageReceived"));
				}
			}
			

		}
	};

	/**
	 * Method called when a new message come in
	 * @param message message that comes in
	 */
	protected abstract void processMessage(Message message);

	/**
	 * Indicate if conditions to go to next step are fulfilled
	 * @return true if conditions are fulfilled, false otherwise
	 */
	protected boolean readyToGoToNextState(){
		if(!this.actionRan) return false;
		for(Participant p: this.dm.getProtocolParticipants().values()){
			if(!this.messagesReceived.containsKey(p.getIpAddress())){
				return false;
			}
		}
		return true;
	}

	/**
	 * Implement logic before going and requesting to go to next state
	 */
	protected abstract void goToNextState();


	/**
	 * Helper method used to send a message
	 * 
	 * @param messageContent content of the message to send
	 * @param type message type
	 * @param IP address to sent the message to if it has to be sent as unicast
	 */
	protected void sendMessage(String messageContent, int type, String ipAddress) {
		if(!dm.isActive())return;
		Intent intentToSend = new Intent("sendMessage");
		intentToSend.putExtra("messageContent", messageContent);
		intentToSend.putExtra("type", type);
		if(ipAddress!=null){
			intentToSend.putExtra("ipAddress", ipAddress);
		}
		LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intentToSend);
	}

	/* Timer methods */

	/**
	 * Start the timer used as time out
	 * @param time time out time in milliseconds
	 * @param numberOfResend number of resend request to send before forcing transition to next step
	 */
	protected void startTimer(long time, int numberOfResend) {
		if (!timerIsRunning) {
			timer = new Timer();
		
			timerTask = new TaskTimer(numberOfResend);
			timer.schedule(timerTask, time);
			timerIsRunning = true;
		}
	}

	/**
	 * Stop the timer used as time out
	 */
	protected void stopTimer() {
		if(timer!=null){
			timerTask.cancel();
			timer.cancel();
			timer.purge();
			timerIsRunning = false;
		}
	}

	/**
	 * Task run on timer tick
	 * 
	 */
	private class TaskTimer extends TimerTask {

		private int numberOfResend;

		public TaskTimer(int numberOfResend){
			this.numberOfResend = numberOfResend;
		}

		@Override
		public void run() {
			if(!dm.isActive()) return;
			if(actionTerminated){
				logger.warn(classname +" Action was called by the tick of the timer, but was already terminated");
				return;
			}
			numberOfTimerRuns++;

			//Ask some other participant to resend their message
			//Ask numberOfResend times
			if(numberOfTimerRuns <= this.numberOfResend){
				logger.debug(classname +" Timer timed out for "+dm.getMyIdentification()+". Asking for resend");
				HashMap<String,Participant> participants;
				if(dm.getProtocolParticipants()!=null){
					participants = dm.getProtocolParticipants();
				}
				else{
					participants = dm.getTempParticipants();
				}
				//Send a resend request (unicast) to each participant which don't appear in the messagesReceived map
				Iterator<Participant> it = participants.values().iterator();
				while (it.hasNext()) {
					Participant p = (Participant)it.next();
					String ipAddress = p.getIpAddress();
					if(!messagesReceived.containsKey(ipAddress) && !ipAddress.equals(dm.getMyIpAddress())){
						sendMessage(RESEND_REQUEST, Integer.valueOf(VotingMessage.RESEND_REQUEST+""+messageTypeToListenTo), ipAddress);
						logger.debug(classname +" Sending resend request to "+ p.getIdentification() +" ("+ipAddress+")");
					}
				}

				stopTimer();
				startTimer(DataManager.TIME_BEFORE_RESEND_REQUESTS, this.numberOfResend);
			}
			//if numberOfResend have been requested, force transition to next step
			else{
				stopTimer();
				//Put the participants from who we didn't receive a message in the excluded list
				for(Participant p: dm.getProtocolParticipants().values()){
					if(!messagesReceived.containsKey(p.getIpAddress()) && !p.equals(dm.getMe())){
						dm.getExcludedParticipants().add(p);
						p.setState(-1);
						logger.debug(classname +" Excluding participant: "+p.getIdentification());
					}
				}
				//remove the excluded participants from the protocol participants
				for(Participant p:dm.getExcludedParticipants()){
					dm.getProtocolParticipants().remove(p.getIpAddress());
				}
				goToNextState();
			}

		}
	};

	/**
	 * Unregister LocalBoradcastReceivers
	 */
	public void reset(){
		this.stopTimer();

		LocalBroadcastManager.getInstance(dm.getContext()).unregisterReceiver(mMessageReceiver);
		LocalBroadcastManager.getInstance(dm.getContext()).unregisterReceiver(participantsLeaved);
	}
	
	public void executeCallback(Intent data) {
		throw new UnsupportedOperationException("Method has not been overridden");
	}
	
	protected void resetParticipantsState(){
		for(Participant p:dm.getProtocolParticipants().values()){
			//If participant has not been declared inactive,
			//set it to Not Yet Send state
			if(p.getState()!=-1){
				p.setState(0);
			}
			//Notify GUI of participant state change
			Intent intent = new Intent();
			intent.setAction("participantMessageReceived");
			LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intent);
		}
	}
	

}
