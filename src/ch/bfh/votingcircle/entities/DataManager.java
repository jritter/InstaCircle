package ch.bfh.votingcircle.entities;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ch.bfh.unicrypt.math.element.interfaces.AtomicElement;
import ch.bfh.unicrypt.math.group.classes.GStarSaveClass;
import ch.bfh.unicrypt.math.group.classes.ZPlusModClass;
import ch.bfh.unicrypt.math.group.interfaces.GStarSave;
import ch.bfh.unicrypt.math.group.interfaces.ZPlusMod;
import ch.bfh.votingcircle.ElectionInfoActivity;
import ch.bfh.votingcircle.EvotingMainActivity;
import ch.bfh.votingcircle.ResultActivity;
import ch.bfh.votingcircle.util.SerializationUtil;

import android.content.Context;

/**
 * Class containing all the data necessary for the protocol and the application
 * @author Philémon von Bergen
 *
 */
public class DataManager {

	//Number of resend request to send if a message has been lost
	public static final int NUMBER_OF_RESEND_REQUESTS=6;
	//Time between two resend requests
	public static final int TIME_BEFORE_RESEND_REQUESTS=3000;
	public static final int VOTING_TIME = 60000;
	
	public static final Logger LOGGER = Logger.getLogger(InstaCircleInterface.class);
	public static final Level LEVEL = Level.DEBUG;
	
	//Protocol
	private SerializationUtil util;
	private String myIdentification;
	private String myIpAddress;
	private String initiator;
	private boolean protocolStarted;
	private boolean active;
	
	//Participants
	private HashMap<String,Participant> protocolParticipants;
	private HashMap<String,Participant> tempParticipants;
	private List<Participant> excludedParticipants = new ArrayList<Participant>();
	private Participant me = null;
	
	//Candidates
	private String question = "";
	private List<Candidate> candidates = new ArrayList<Candidate>();
	private List<Candidate> result;
	
	//Crypto
	//BigInteger.valueOf(23);//
	private BigInteger p = new BigInteger("139700725455163817218350367330248482081469054544178136562919376411664993761516359902466877562980091029644919043107300384411502932147505225644121231955823457305399024737939358832740762188317942235632506234568870323910594575297744132090375969205586671220221046438363046016679285179955869484584028329654326524819");
	private BigInteger q;
	private GStarSave bigG_q;
	private ZPlusMod bigZ_q;
	private AtomicElement generator;
	private Random random = new SecureRandom();

	//Android
	private Context context;
	private ElectionInfoActivity electionInfoActivity;
	private EvotingMainActivity evotingMainActivity;
	private ResultActivity resultActivity;

	
	/**
	 * Create a DataManager object
	 */
	public DataManager(){
		bigG_q = new GStarSaveClass(this.p, true);
		q = bigG_q.getOrder();
		bigZ_q = new ZPlusModClass(q);
	}

	/**
	 * Get the identification of the user
	 * @return the identification
	 */
	public String getMyIdentification() {
		return myIdentification;
	}

	/**
	 * Set the identity of the user
	 * @param identification
	 */
	public void setMyIdentification(String identification) {
		this.myIdentification = identification;
	}

	/**
	 * Get the ip address of the user
	 * @return the ip address
	 */
	public String getMyIpAddress() {
		return myIpAddress;
	}

	/**
	 * Set the ip address of the user
	 * @param ipAddress
	 */
	public void setMyIpAddress(String ipAddress) {
		this.myIpAddress = ipAddress;
	}

	/**
	 * Get the ip address of the initiator of the protocol
	 * @return ip address of the initiator of the protocol
	 */
	public String getInitiator() {
		return initiator;
	}

	/**
	 * Set the ip address of the initiator of the protocol
	 * @param initiator ip address of the initiator of the protocol
	 */
	public void setInitiator(String initiator) {
		this.initiator = initiator;
	}

	/**
	 * Indicate if the protocol is started or not
	 * @return true if started, false otherwise
	 */
	public boolean isProtocolStarted() {
		return protocolStarted;
	}

	/**
	 * Set state of the protocol
	 * @param protocolStarted True if started, false otherwise
	 */
	public void setProtocolStarted(boolean protocolStarted) {
		this.protocolStarted = protocolStarted;
	}

	/**
	 * Indicate if this DataManager is still active
	 * It must be set to inactive when it is dereferenced 
	 * @return true if active, false otherwise
	 */
	public boolean isActive() {
		/*
		 * if the attached activity is null, this means it has been destroyed, so this
		 * data manager is no more attached to the application
		 */
		if(this.evotingMainActivity==null)active=false;
		return active;
	}

	/**
	 * Indicate if this DataManager is still active
	 * It must be set to inactive when it is dereferenced 
	 * @param active true if active, false otherwise
	 */
	public void setActive(boolean active) {
		this.active = active;
	}
	
	/**
	 * Get the participants to the protocol
	 * @return the participants to the protocol
	 */
	public HashMap<String,Participant> getProtocolParticipants() {
		return protocolParticipants;
	}

	/**
	 * Set the the participants to the protocol
	 * @param participants the participants to the protocol
	 */
	public void setProtocolParticipants(HashMap<String,Participant> participants) {
		this.protocolParticipants = participants;
		this.me = this.protocolParticipants.get(this.getMyIpAddress());
	}
	
	/**
	 * Set participants connected to the network before start of the protocol
	 * @param participants participants connected to the network before start of the protocol
	 */
	public void setTempParticipants(HashMap<String,Participant> participants) {
		this.tempParticipants = participants;
	}
	
	/**
	 * Get participants connected to the network before start of the protocol
	 * @return participants connected to the network before start of the protocol
	 */
	public HashMap<String,Participant> getTempParticipants(){
		return this.tempParticipants;
	}

	/**
	 * Get the participants excluded from the protocol
	 * @return the participants excluded from the protocol
	 */
	public List<Participant> getExcludedParticipants() {
		return excludedParticipants;
	}

	/**
	 * Set the participants excluded from the protocol
	 * @param excludedParticipants the participants excluded from the protocol
	 */
	public void setExcludedParticipants(List<Participant> excludedParticipants) {
		this.excludedParticipants = excludedParticipants;
	}

	/**
	 * Get the Participant Object representing the user
	 * @return the Participant Object representing the user
	 */
	public Participant getMe() {
		return me;
	}
	
	/**
	 * Set the candidates that can be elected
	 * @param candidates the candidates that can be elected
	 */
	public void setCandidates(List<Candidate> candidates) {
		this.candidates=candidates;
	}
	
	/**
	 * Get the candidates that can be elected
	 * @return the candidates that can be elected
	 */
	public List<Candidate> getCandidates() {
		return this.candidates;
	}

	/**
	 * Set the question for the poll
	 * @param question question for the poll
	 */
	public void setQuestion(String question) {
		this.question = question;
	}
	
	/**
	 * Get the question of the poll
	 * @return the question of the poll
	 */
	public String getQuestion() {
		return this.question;
	}

	/**
	 * Set the result of the poll
	 * @param result the result of the poll
	 */
	public void setResult(List<Candidate> result) {
		this.result=result;
	}

	/**
	 * Get the result of the poll
	 * @return the result of the poll
	 */
	public List<Candidate> getResult() {
		return result;
	}
	

	/**
	 * Get the safe prime group used in the protocol
	 * @return the safe prime group used in the protocol
	 */
	public GStarSave getG_q() {
		return bigG_q;
	}

	/**
	 * Get the additive group used in the protocol
	 * @return the additive group used in the protocol
	 */
	public ZPlusMod getZ_q() {
		return bigZ_q;
	}

	/**
	 * Get the generator of the group Gq used in the protocol
	 * @return the generator of the group Gq used in the protocol
	 */
	public AtomicElement getGenerator() {
		return generator;
	}

	/**
	 * Set the generator of the group Gq used in the protocol
	 * @param generator the generator of the group Gq used in the protocol
	 */
	public void setGenerator(AtomicElement generator) {
		this.generator = generator;
	}
	
	/**
	 * Get a secure random object
	 * @return
	 */
	public Random getRandom() {
		return random;
	}

	/**
	 * Set the Serialization context class
	 * @return Serialization context class
	 */
	public SerializationUtil getSerializationUtil() {
		return util;
	}

	/**
	 * Get the Serialization context class
	 * @param serializationUtil Serialization context class
	 */
	public void setSerializationUtil(SerializationUtil serializationUtil) {
		this.util = serializationUtil;
	}

	/**
	 * Set the activity allowing to determine the poll informations
	 * @param electionInfoActivity
	 */
	public void setElectionInfoActivity(ElectionInfoActivity electionInfoActivity) {
		this.electionInfoActivity = electionInfoActivity;
	}
	
	/**
	 * Get the activity allowing to determine the poll informations
	 * @return
	 */
	public ElectionInfoActivity getElectionInfoActivity() {
		return this.electionInfoActivity;
	}

	/**
	 * Set the Evoting main activity
	 * @param evotingMainActivity
	 */
	public void setEvotingMainActivity(EvotingMainActivity evotingMainActivity) {
		this.evotingMainActivity = evotingMainActivity;
		if(evotingMainActivity!=null) active=true;
	}
	
	/**
	 * Get the Evoting main activity
	 * @return
	 */
	public EvotingMainActivity getEvotingMainActivity() {
		return this.evotingMainActivity;
	}

	/**
	 * Set the activity displaying the results
	 * @param resultActivity
	 */
	public void setResultActivity(ResultActivity resultActivity) {
		this.resultActivity = resultActivity;
	}

	/**
	 * Get the activity displaying the results
	 * @return
	 */
	public ResultActivity getResultActivity() {
		return this.resultActivity;
	}
	
	/**
	 * Get the Android context of the application
	 * @return the Android context of the application
	 */
	public Context getContext() {
		return context;
	}

	/**
	 * Set the Android context of the application
	 * @param context the Android context of the application
	 */
	public void setContext(Context context) {
		this.context = context;
	}
	
}
