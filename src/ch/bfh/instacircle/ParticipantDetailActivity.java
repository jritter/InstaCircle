package ch.bfh.instacircle;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import ch.bfh.instacircle.db.NetworkDbHelper;

public class ParticipantDetailActivity extends FragmentActivity {

	private int participantId;
	private NetworkDbHelper dbHelper;
	private String ipAddress;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		dbHelper = new NetworkDbHelper(this);
		participantId = getIntent().getIntExtra("participant_id", -1);

		Cursor participant = dbHelper.queryParticipant(participantId);
		participant.moveToFirst();

		setTitle(participant.getString(participant
				.getColumnIndex("identification")));
		ipAddress = participant.getString(participant
				.getColumnIndex("ip_address"));

		setContentView(R.layout.activity_participant_detail);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_participant_detail, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public String getIpAddress() {
		return ipAddress;
	}
	
}
