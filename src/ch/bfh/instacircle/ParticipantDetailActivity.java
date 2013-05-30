/*
 *  UniCrypt Cryptographic Library
 *  Copyright (c) 2013 Berner Fachhochschule, Biel, Switzerland.
 *  All rights reserved.
 *
 *  Distributable under GPL license.
 *  See terms of license at gnu.org.
 *  
 */

package ch.bfh.instacircle;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import ch.bfh.instacircle.db.NetworkDbHelper;

/**
 * Activity which displays the details (ParticipantDetailFragment) and the
 * MessageSendFragment in order to send Unicast messages to he participant
 * 
 * @author Juerg Ritter (rittj1@bfh.ch)
 */
public class ParticipantDetailActivity extends FragmentActivity {

	private int participantId;
	private NetworkDbHelper dbHelper;
	private String ipAddress;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// getting access to the database and query it
		dbHelper = NetworkDbHelper.getInstance(this);
		participantId = getIntent().getIntExtra("participant_id", -1);

		Cursor participant = dbHelper.queryParticipant(participantId);
		participant.moveToFirst();

		setTitle(participant.getString(participant
				.getColumnIndex("identification")));
		ipAddress = participant.getString(participant
				.getColumnIndex("ip_address"));

		// applying the view
		setContentView(R.layout.activity_participant_detail);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_participant_detail, menu);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Returns the IP address of the currently displayed participant. This is
	 * used by the MessageSendFragment
	 * 
	 * @return the IP address of the currently displayed participant
	 */
	public String getIpAddress() {
		return ipAddress;
	}

}
