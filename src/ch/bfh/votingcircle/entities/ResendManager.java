package ch.bfh.votingcircle.entities;

import org.apache.log4j.Logger;

import ch.bfh.instacircle.Message;
import ch.bfh.unicrypt.math.element.interfaces.AtomicElement;
import ch.bfh.unicrypt.math.element.interfaces.Element;
import ch.bfh.votingcircle.statemachine.actions.VotingMessage;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Class responsible to resend message that have not been received by other participants
 * @author Philémon von Bergen
 *
 */
public class ResendManager {

	private DataManager dm;
	private Logger logger;

	public ResendManager(DataManager dm) {
		this.dm = dm;
		this.logger = DataManager.LOGGER;
		
		// Subscribing to the messageArrived events to receive resend requests
		LocalBroadcastManager.getInstance(dm.getContext()).registerReceiver(
										this.mMessageReceiver, new IntentFilter("messageArrived"));
	}
	
	/**
	 * When quitting the application, the resend manager has to been reseted
	 */
	public void close(){
		LocalBroadcastManager.getInstance(this.dm.getContext()).unregisterReceiver(mMessageReceiver);
	}

	/**
	 * Receive resend request messages 
	 */
	protected BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(!dm.isActive())return;
			//Get the message
			Bundle messages = intent.getExtras();
			Message message = (Message) messages.get("message");
			
			//Do not resend to me
			if(message.getSenderIPAddress().equals(dm.getMyIpAddress())){
				return;
			}
			
			//do not resend messages to a participant that is not in the participants
			Participant excluded = null;
			for(Participant p:dm.getExcludedParticipants()){
				if(p.getIpAddress().equals(message.getSenderIPAddress())){
					excluded=p;
				}
			}
			if(dm.getProtocolParticipants()!=null && !(dm.getProtocolParticipants().containsKey(message.getSenderIPAddress())
					|| dm.getExcludedParticipants().contains(excluded))){
				return;
			}
					
			//If the message is a resend request
			if (Math.floor(message.getMessageType()/100) == VotingMessage.RESEND_REQUEST) {
				logger.debug(this.getClass().getSimpleName() +" message received with type "+message.getMessageType()+ " from "+message.getSender());
				switch(message.getMessageType()%100){
				
				case VotingMessage.MSG_CONTENT_START:
					if(dm.getInitiator()!=null && dm.getMyIpAddress()!=null &&
						dm.getInitiator().equals(dm.getMyIpAddress()) && dm.isProtocolStarted()){
						
						//send message to start protocol containing the participants to the protocol
						Intent intentMessage = new Intent("sendMessage");
						intentMessage.putExtra("type", VotingMessage.MSG_CONTENT_START);
						LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intentMessage);
						logger.debug(this.getClass().getSimpleName()+" Resending Start Message");
					} else {
						//Not sending in this case
						logger.debug(this.getClass().getSimpleName()+" Not the initiator or protocol not started");
					}
					break;
				case VotingMessage.MSG_CONTENT_INIT:
					AtomicElement g = dm.getGenerator();
					if(g!=null && dm.getProtocolParticipants()!=null && dm.getCandidates()!=null && dm.getQuestion()!=null){
						String gString = dm.getSerializationUtil().serialize(g);
						String participantsString = dm.getSerializationUtil().serialize(dm.getProtocolParticipants());
						String candidateString = dm.getSerializationUtil().serialize(dm.getCandidates());
						String questionString = dm.getSerializationUtil().serialize(dm.getQuestion());
						Intent intentToSend = new Intent("sendMessage");
						intentToSend.putExtra("messageContent", gString+"|"+participantsString+"|"+candidateString+"|"+questionString);
						intentToSend.putExtra("type", VotingMessage.MSG_CONTENT_INIT);
						LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intentToSend);
						logger.debug(this.getClass().getSimpleName()+" Resending Init Message");
					} else {
						logger.debug(this.getClass().getSimpleName()+" Not resending value as they weren't computed");
					}
					break;
				case VotingMessage.MSG_CONTENT_SETUP:
					if(dm.getMe()==null){
						logger.debug(this.getClass().getSimpleName()+" Not resending value as they weren't computed");
						break;
					}
					Element ai = dm.getMe().getAi();
					Element proof = dm.getMe().getProofForXi(); 
					if(ai!=null && proof !=null){
						//Send a_i and the proof to other participants
						String aiString = dm.getSerializationUtil().serialize(ai);
						String proofString = dm.getSerializationUtil().serialize(proof);
						
						Intent intentToSend = new Intent("sendMessage");
						intentToSend.putExtra("messageContent", aiString+"|"+proofString);
						intentToSend.putExtra("type", VotingMessage.MSG_CONTENT_SETUP);
						LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intentToSend);
						logger.debug(this.getClass().getSimpleName()+" Resending Setup Message");
					} else {
						logger.debug(this.getClass().getSimpleName()+" Not resending value as they weren't computed");
					}
					break;
				case VotingMessage.MSG_CONTENT_COMMIT:
					if(dm.getMe()==null){
						logger.debug(this.getClass().getSimpleName()+" Not resending value as they weren't computed");
						break;
					}
					Element hi = dm.getMe().getHi();
					Element proofVote = dm.getMe().getProofValidVote();
					if(hi!=null && proofVote!=null){
						String hiString = dm.getSerializationUtil().serialize(hi);
						String proofString = dm.getSerializationUtil().serialize(proofVote);
						Intent intentToSend = new Intent("sendMessage");
						intentToSend.putExtra("messageContent", hiString+"|"+proofString);
						intentToSend.putExtra("type", VotingMessage.MSG_CONTENT_COMMIT);
						LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intentToSend);
						logger.debug(this.getClass().getSimpleName()+" Resending Commit Message");
					} else {
						logger.debug(this.getClass().getSimpleName()+" Not resending value as they weren't computed");
					}
					break;
				case VotingMessage.MSG_CONTENT_VOTE:
					if(dm.getMe()==null){
						logger.debug(this.getClass().getSimpleName()+" Not resending value as they weren't computed");
						break;
					}
					Element bi= dm.getMe().getBi();
					if(bi!=null){
						//Send b_i
						String biString = dm.getSerializationUtil().serialize(bi);
						Intent intentToSend = new Intent("sendMessage");
						intentToSend.putExtra("messageContent", biString);
						intentToSend.putExtra("type", VotingMessage.MSG_CONTENT_VOTE);
						LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intentToSend);
						logger.debug(this.getClass().getSimpleName()+" Resending Vote Message");
					} else {
						logger.debug(this.getClass().getSimpleName()+" Not resending value as they weren't computed");
					}
					break;
				case VotingMessage.MSG_CONTENT_RECOVER:
					if(dm.getMe()==null){
						logger.debug(this.getClass().getSimpleName()+" Not resending value as they weren't computed");
						break;
					}
					Element hiHat = dm.getMe().getHiHat();
					Element hiHatPowXi = dm.getMe().getHiHatPowXi();
					Element proofHi = dm.getMe().getProofForHiHat();
					if(hiHat!=null && hiHatPowXi!=null && proofHi!=null){
						
						String hiHatString = dm.getSerializationUtil().serialize(hiHat);
						String hiHatPowXiString = dm.getSerializationUtil().serialize(hiHatPowXi);
						String proofString = dm.getSerializationUtil().serialize(proofHi);
						
						Intent intentToSend = new Intent("sendMessage");
						intentToSend.putExtra("messageContent", hiHatString+"|"+hiHatPowXiString+"|"+proofString);
						intentToSend.putExtra("type", VotingMessage.MSG_CONTENT_RECOVER);
						LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intentToSend);
						logger.debug(this.getClass().getSimpleName()+" Resending Recovery Message");
					} else {
						logger.debug(this.getClass().getSimpleName()+" Not resending value as they weren't computed");
					}
					break;
				}
			}
		}
	};
}
