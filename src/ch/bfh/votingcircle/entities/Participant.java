package ch.bfh.votingcircle.entities;

import java.io.Serializable;

import ch.bfh.unicrypt.math.element.interfaces.AtomicElement;
import ch.bfh.unicrypt.math.element.interfaces.Element;

/**
 * Class representing a participant to the protocol
 * @author Philémon von Bergen
 *
 */
public class Participant implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private String ipAddress;
	private String identification;
	
	//State: -1: excluded, 0: waiting for message, 1: message received
	private int state = 0;
	
	private int protocolParticipantIndex;
	
	private Element xi = null;
	private Element ai = null;
	private Element proofForXi = null;
	private Element hi = null;
	private Element bi = null;
	private Element proofValidVote = null;
	private Element hiHat = null;
	private Element hiHatPowXi = null;
	private Element proofForHiHat = null;
	
	/**
	 * Construct a participant object with her ip address and her identification
	 * @param ipAddress
	 * @param identification
	 */
	public Participant(String ipAddress, String identification){
		this.ipAddress = ipAddress;
		this.identification = identification;
	}

	/**
	 * Get the ip address of the participant
	 * @return  the ip address of the participant
	 */
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * Set the ip address of the participant
	 * @param ipAddress the ip address of the participant
	 */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	/**
	 * Get the identification of the participant
	 * @return the identification of the participant
	 */
	public String getIdentification() {
		return identification;
	}

	/**
	 * Set the identification of the participant
	 * @param identification the identification of the participant
	 */
	public void setIdentification(String identification) {
		this.identification = identification;
	}

	/**
	 * Get state of a participant
	 * @return -1: excluded, 0: waiting for message, 1: message received
	 */
	public int getState() {
		return state;
	}

	/**
	 * Set the state of a participant
	 * @param state -1: excluded, 0: waiting for message, 1: message received
	 */
	public void setState(int state) {
		this.state = state;
	}
	
	/**
	 * Get the index of the participant in the protocol
	 * @return identification integer
	 */
	public int getProtocolParticipantIndex() {
		return protocolParticipantIndex;
	}

	/**
	 * Set the index of the participant in the protocol
	 * @return identification integer
	 */
	public void setProtocolParticipantIndex(int protocolParticipantIndex) {
		this.protocolParticipantIndex = protocolParticipantIndex;
	}

	/**
	 * Get value x of the protocol for this participant
	 * @return
	 */
	public Element getXi() {
		return xi;
	}

	/**
	 * Set value x of the protocol for this participant
	 * @param xi
	 */
	public void setXi(Element xi) {
		this.xi = xi;
	}
	
	/**
	 * Get value a of the protocol for this participant
	 * @return
	 */
	public Element getAi() {
		return ai;
	}

	/**
	 * Set value a of the protocol for this participant
	 * @param ai
	 */
	public void setAi(Element ai) {
		this.ai = ai;
	}

	/**
	 * Get the ZK proof for knowledge of x
	 * @return
	 */
	public Element getProofForXi() {
		return proofForXi;
	}

	/**
	 * Set the ZK proof for knowledge of x
	 * @param proofForXi
	 */
	public void setProofForXi(Element proofForXi) {
		this.proofForXi = proofForXi;
	}

	/**
	 * Get value h of the protocol for this participant
	 * @return
	 */
	public Element getHi() {
		return hi;
	}

	/**
	 * Set value h of the protocol for this participant
	 * @param hi
	 */
	public void setHi(Element hi) {
		this.hi = hi;
	}

	/**
	 * Get value b of the protocol for this participant
	 * @return
	 */
	public Element getBi() {
		return bi;
	}

	/**
	 * Set value b of the protocol for this participant
	 * @param bi
	 */
	public void setBi(Element bi) {
		this.bi = bi;
	}

	/**
	 * Get the validity proof for the vote
	 * @return
	 */
	public Element getProofValidVote() {
		return proofValidVote;
	}

	/**
	 * Set the validity proof for the vote
	 * @param proofValidVote
	 */
	public void setProofValidVote(Element proofValidVote) {
		this.proofValidVote = proofValidVote;
	}
	
	/**
	 * Get value h hat of the protocol for this participant
	 * @return
	 */
	public Element getHiHat() {
		return hiHat;
	}

	/**
	 * Set value h hat of the protocol for this participant
	 * @param hiHat
	 */
	public void setHiHat(Element hiHat) {
		this.hiHat = hiHat;
	}

	/**
	 * Get value (h hat)^x of the protocol for this participant
	 * @return
	 */
	public Element getHiHatPowXi() {
		return hiHatPowXi;
	}

	/**
	 * Set value (h hat)^x of the protocol for this participant
	 * @param hiHatPowXi
	 */
	public void setHiHatPowXi(Element hiHatPowXi) {
		this.hiHatPowXi = hiHatPowXi;
	}

	/**
	 * Get the Equality between logs ZK proof 
	 * @return
	 */
	public Element getProofForHiHat() {
		return proofForHiHat;
	}

	/**
	 * Set the Equality between logs ZK proof
	 * @param proofForHiHat
	 */
	public void setProofForHiHat(Element proofForHiHat) {
		this.proofForHiHat = proofForHiHat;
	}

	@Override
	public String toString() {
		String s = "Participant object\n";
		s+="\tIdentification: "+this.identification+"\n";
		s+="\tIP Address: "+this.ipAddress+"\n";
		s+="\tState at this moment: "+this.state+"\n";
		s+="\tProtocol participant index: "+this.protocolParticipantIndex+"\n";
		if(this.xi!=null){
			s+="\txi: "+((AtomicElement)this.xi).getBigInteger()+"\n";
		} else {
			s+="\txi: "+this.xi+"\n";
		}
		if(this.ai!=null){
			s+="\tai: "+((AtomicElement)this.ai).getBigInteger()+"\n";
		} else {
			s+="\tai: "+this.ai+"\n";
		}
		if(this.bi!=null){
			s+="\tbi: "+((AtomicElement)this.bi).getBigInteger()+"\n";
		} else {
			s+="\tbi: "+this.bi+"\n";
		}
		if(this.hi!=null){
			s+="\thi: "+((AtomicElement)this.hi).getBigInteger()+"\n";
		} else {
			s+="\thi: "+this.hi+"\n";
		}
		if(this.hiHat!=null){
			s+="\thi hat: "+((AtomicElement)this.hiHat).getBigInteger()+"\n";
		} else {
			s+="\thi hat: "+this.hiHat+"\n";
		}
		if(this.hiHatPowXi!=null){
			s+="\thi hat pow xi: "+((AtomicElement)this.hiHatPowXi).getBigInteger()+"\n";
		} else {
			s+="\thi hat pow xi: "+this.hiHatPowXi+"\n";
		}
		if(this.proofForXi!=null){
			s+="\tProof for xi: "+this.proofForXi+"\n";
		} else {
			s+="\tProof for xi: "+this.proofForXi+"\n";
		}
		if(this.proofValidVote!=null){
			s+="\tProof of valid vote: "+this.proofValidVote+"\n";
		} else {
			s+="\tProof of valid vote: "+this.proofValidVote+"\n";
		}
		if(this.proofValidVote!=null){
			s+="\tProof for hi hat pow xi : "+this.proofForHiHat+"\n";
		} else {
			s+="\tProof for hi hat pow xi : "+this.proofForHiHat+"\n";
		}
		return s;
	}

	@Override
	public boolean equals(Object o) {
		if(o==null) return false;
		if(!(o instanceof Participant)) return false;
		Participant other = (Participant)o;
		
		if(this.identification.equals(other.identification) && this.ipAddress.equals(other.ipAddress)){
			return true;
		}

		return false;
	}
}
