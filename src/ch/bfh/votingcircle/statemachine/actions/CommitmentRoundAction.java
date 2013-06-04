package ch.bfh.votingcircle.statemachine.actions;


import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import ch.bfh.instacircle.Message;
import ch.bfh.instacircle.R;
import ch.bfh.unicrypt.encryption.classes.ElGamalEncryptionClass;
import ch.bfh.unicrypt.math.element.interfaces.AtomicElement;
import ch.bfh.unicrypt.math.element.interfaces.Element;
import ch.bfh.votingcircle.EvotingMainActivity;
import ch.bfh.votingcircle.VoteActivity;
import ch.bfh.votingcircle.entities.DataManager;
import ch.bfh.votingcircle.entities.Participant;
import ch.bfh.votingcircle.statemachine.events.AllCommitMessagesReceivedEvent;

import com.continuent.tungsten.fsm.core.Entity;
import com.continuent.tungsten.fsm.core.Event;

import com.continuent.tungsten.fsm.core.Transition;
import com.continuent.tungsten.fsm.core.TransitionFailureException;
import com.continuent.tungsten.fsm.core.TransitionRollbackException;

/**
 * Action executed in Commit step
 * This action show a UI to the voter so she can choose her vote, and the do some computation with the vote
 * @author Philémon von Bergen
 * 
 */
public class CommitmentRoundAction extends AbstractAction {

	private int messageTypeToListenTo;

	public CommitmentRoundAction(int messageTypeToListenTo, DataManager dm) {
		super(messageTypeToListenTo,dm);
		this.classname = this.getClass().getSimpleName(); 
		this.messageTypeToListenTo = messageTypeToListenTo;
	}

	@Override
	public void doAction(Event message, Entity entity, Transition transition,
			int actionType) throws TransitionRollbackException,
			TransitionFailureException {

		if(!dm.isActive()) return;
		
		EvotingMainActivity activity = this.dm.getEvotingMainActivity();
		activity.setActivityTitle(R.string.commitment_round);
		activity.changeImage(R.id.flow, R.drawable.flow_4);

		logger.debug("CommitmentRoundAction started");

		//Listen to participant leaved events
		IntentFilter intentFilter = new IntentFilter("participantChangedState");
		LocalBroadcastManager.getInstance(dm.getContext()).registerReceiver(participantsLeft, intentFilter);

		//compute the values of the protocol
		logger.debug(this.classname +" Computing and sending protocol values");

		Participant me = this.dm.getMe();

		//compute the values of the protocol

		Element productNumerator = this.dm.getG_q().createElement(BigInteger.valueOf(1));
		Element productDenominator = this.dm.getG_q().createElement(BigInteger.valueOf(1));
		for(Participant p: this.dm.getProtocolParticipants().values()){
			if(p.getProtocolParticipantIndex() < me.getProtocolParticipantIndex()){
				productNumerator = productNumerator.apply(p.getAi());
			} else if (p.getProtocolParticipantIndex() > me.getProtocolParticipantIndex()){
				productDenominator = productDenominator.apply(p.getAi());
			}
		}
		Element hi = productNumerator.apply(productDenominator.invert());
		me.setHi(hi);

		logger.debug(this.classname +" showing Vote UI");

		activity.dismissProgressDialog();
		activity.showActivity(VoteActivity.class,this);

	}

	@Override
	public void executeCallback(Intent data) {
		logger.debug(this.classname +" Result received from VoteActivity");

		EvotingMainActivity activity = this.dm.getEvotingMainActivity();
		activity.showProgressDialog(activity.getString(R.string.progress_dialog_title), activity.getString(R.string.progress_dialog_text));

		int index = data.getIntExtra("index", -1);

		//possiblePlainTexts are the possible vi
		AtomicElement[] possiblePlainTexts = new AtomicElement[this.dm.getCandidates().size()];
		for(int i=0;i<this.dm.getCandidates().size();i++){
			possiblePlainTexts[i]=(AtomicElement)this.dm.getCandidates().get(i).getRepresentation();
		}

		//possibleVotes are the g^vi
		AtomicElement[] possibleVotes = new AtomicElement[this.dm.getCandidates().size()];
		for(int i=0;i<this.dm.getCandidates().size();i++){
			possibleVotes[i]=this.dm.getGenerator().selfApply(possiblePlainTexts[i]);
		}

		if(index!=-1 && index<possiblePlainTexts.length){

			Participant me = this.dm.getMe();

			AtomicElement vi = possiblePlainTexts[index];

			Element bi = me.getHi().selfApply(((AtomicElement)me.getXi())).apply(this.dm.getGenerator().selfApply(vi));
			me.setBi(bi);

			//compute validity proof
			ElGamalEncryptionClass eec = new ElGamalEncryptionClass(this.dm.getG_q(), this.dm.getGenerator());
			Element proof = eec.createValididtyProof(this.dm.getMe().getXi(), this.dm.getMe().getHi(), index, this.dm.getRandom(), possibleVotes);
			me.setProofValidVote(proof);

			//Send the proof (commitment to the vote) to other participants
			String hiString = this.dm.getSerializationUtil().serialize(me.getHi());
			String proofString = this.dm.getSerializationUtil().serialize(proof);

			//Notify GUI of participant state change
			me.setState(1);
			Intent intent = new Intent();
			intent.setAction("participantMessageReceived");
			LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intent);

			this.sendMessage(hiString+"|"+proofString,VotingMessage.MSG_CONTENT_COMMIT, null);
		} else {
			this.logger.warn(this.classname+" Invalid vote");
		}

		this.actionRan = true;

		//Voting phase can be long. But if all voter vote, we don't have to wait the whole vote time
		//In order not to wait the whole vote time if a messae has been missed, we already send resend request before vote time times out
		this.startTimerVote(DataManager.TIME_BEFORE_RESEND_REQUESTS);

		if(this.readyToGoToNextState()){
			this.goToNextState();
		} else {
			startTimer(DataManager.VOTING_TIME, DataManager.NUMBER_OF_RESEND_REQUESTS);
			logger.debug(this.classname +" Waiting for other participant. Actually received:"
					+ this.messagesReceived.size() +" of "+ this.dm.getProtocolParticipants().size());
		}
	}


	@Override
	protected void processMessage(Message message){

		Participant p = dm.getProtocolParticipants().get(message.getSenderIPAddress());
		
		//p can be null if participant that sent message has been previously excluded
		if(p==null){
			return;
		}

		if(!p.equals(this.dm.getMe())){

			//Store value received in the corresponding participant object
			StringTokenizer tokenizer = new java.util.StringTokenizer(message.getMessage(), "|");

			//if message is malformed
			if(tokenizer.countTokens()!=2){
				this.messagesReceived.remove(message.getSenderIPAddress());
				logger.warn(this.classname+" Commit message was not composed of two parts");
				//start timer and return;
				startTimer(DataManager.TIME_BEFORE_RESEND_REQUESTS, DataManager.NUMBER_OF_RESEND_REQUESTS);
				return;
			}
			else{
				String hiString = tokenizer.nextToken();
				String proofString = tokenizer.nextToken();

				Element hi = (Element)this.dm.getSerializationUtil().deserialize(hiString);
				Element proof = (Element)this.dm.getSerializationUtil().deserialize(proofString);

				//store the value in the corresponding participant
				p.setHi(hi);
				p.setProofValidVote(proof);
				p.setState(1);

				//Notify GUI of participant state change
				Intent intent = new Intent();
				intent.setAction("participantMessageReceived");
				LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intent);
			}
		}

		if(this.readyToGoToNextState()){
			this.goToNextState();
		}
	}



	@Override
	protected void goToNextState(){
		this.actionTerminated = true;
		
		this.reset();
		this.resetParticipantsState();
		this.stopTimerVote();

		Intent transitionIntent = new Intent("applyTransition");
		transitionIntent.putExtra("event", new AllCommitMessagesReceivedEvent(null));
		LocalBroadcastManager.getInstance(this.dm.getContext()).sendBroadcast(transitionIntent);
	}


	private Timer timerVote;
	private boolean timerIsRunning = false;
	private TimerTask timerTaskVote;

	/**
	 * Start the timer for resend requests
	 * @param time time out time in milliseconds
	 */
	private void startTimerVote(long time) {
		timerVote = new Timer();
		if (!timerIsRunning) {
			timerTaskVote = new TaskTimer();
			timerVote.schedule(timerTaskVote, time);
			timerIsRunning = true;
		}
	}

	/**
	 * Stop the timer used as time out
	 */
	private void stopTimerVote() {
		if(timerVote!=null){
			timerTaskVote.cancel();
			timerVote.cancel();
			timerVote.purge();
			timerIsRunning = false;
		}
	}

	/**
	 * Task run on timer tick
	 * 
	 */
	private class TaskTimer extends TimerTask {

		@Override
		public void run() {
			if(actionTerminated){
				logger.warn(classname +" Action was called by the tick of the vote timer, but was already terminated");
				return;
			}


			logger.debug(classname +" Timer vote timed out for "+dm.getMyIdentification()+". Asking for resend");
			HashMap<String,Participant> participants = dm.getProtocolParticipants();

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

			stopTimerVote();
			startTimerVote(DataManager.TIME_BEFORE_RESEND_REQUESTS);
		}

	};

}
