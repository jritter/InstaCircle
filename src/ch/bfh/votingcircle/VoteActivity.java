package ch.bfh.votingcircle;

import java.util.ArrayList;
import java.util.List;

import ch.bfh.instacircle.R;
import ch.bfh.votingcircle.entities.Candidate;
import ch.bfh.votingcircle.entities.DataManager;
import android.os.Bundle;
import android.app.Activity;

import android.app.ListActivity;
import android.content.Intent;

import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Activity showing the possible vote choices
 * @author Philémon von Bergen
 *
 */
public class VoteActivity extends ListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_vote);
		
		DataManager dm = ((EvotingApplication)this.getApplication()).getDataManager();
		
		// initializing the adapter
		List<String> list = new ArrayList<String>();
		list.add("No");
		list.add("Yes");
		String question = dm.getQuestion();
		TextView tv = (TextView)findViewById(R.id.question);
		tv.setText(question);
		
		List<Candidate> candidates = dm.getCandidates();
		List<String> candidatesString = new ArrayList<String>();
		for(Candidate c:candidates){
			candidatesString.add(c.getText());
		}
		ArrayAdapter<String> aa = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1,candidatesString);
		setListAdapter(aa);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.ListFragment#onListItemClick(android.widget.ListView
	 * , android.view.View, int, long)
	 */
	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);
		
		Intent resultIntent = new Intent();
		resultIntent.putExtra("index",position);
		setResult(Activity.RESULT_OK, resultIntent);
		finish();

	}

}
