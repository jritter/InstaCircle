package ch.bfh.votingcircle.entities;

import java.io.Serializable;

import ch.bfh.unicrypt.math.element.interfaces.Element;

/**
 * Class representing a Candidate or a vote possibility
 * @author Philémon von Bergen
 * 
 */
public class Candidate implements Serializable, Comparable<Candidate>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String text;
	private Element representation;
	private int votes;
	
	/**
	 * Construct a candidate
	 * @param text text representation of the candidate (e.g. Name)
	 * @param representation cryptographic representation of the candidate
	 */
	public Candidate(String text, Element representation){
		this.text=text;
		this.representation=representation;
	}
	
	/**
	 * Get the text representation of the candidate
	 * @return text representation of the candidate
	 */
	public String getText() {
		return text;
	}
	
	/**
	 * Set the text representation of the candidate
	 * @param text text representation of the candidate
	 */
	public void setText(String text) {
		this.text = text;
	}
	
	/**
	 * Get the cryptographic representation of the candidate
	 * @return the cryptographic representation of the candidate
	 */
	public Element getRepresentation() {
		return representation;
	}
	
	/**
	 * Set the cryptographic representation of the candidate
	 * @param representation the cryptographic representation of the candidate
	 */
	public void setRepresentation(Element representation) {
		this.representation = representation;
	}

	/**
	 * Get number of votes this candidate received
	 * @return number of votes received
	 */
	public int getVotes() {
		return votes;
	}

	/**
	 * Set number of votes this candidate received
	 * @param votes number of votes received
	 */
	public void setVotes(int votes) {
		this.votes = votes;
	}

	/**
	 * Comparison method based on number of votes received
	 */
	@Override
	public int compareTo(Candidate other) {
		
		if(this.votes < ((Candidate)other).votes) return -1;
		if(this.votes > ((Candidate)other).votes) return 1;
		return 0;
	}
	
	
}
