package ch.bfh.votingcircle;

import java.util.List;

import ch.bfh.instacircle.R;
import ch.bfh.votingcircle.entities.Candidate;
import ch.bfh.votingcircle.entities.DataManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.ListActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * Activity responsible to display the results of the poll
 * @author Philémon von Bergen
 *
 */
public class ResultActivity extends ListActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_result);
		
		DataManager dm = ((EvotingApplication)this.getApplication()).getDataManager();
		dm.setResultActivity(this);

		final Activity activity = this;
		
		Button btnClose = (Button) this.findViewById(R.id.close_button);
		btnClose.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				activity.finish();
			}
		});
		
		//Get the results and show them
		List<Candidate> result = dm.getResult();
		ResultListAdapter rla = new ResultListAdapter(this.getApplicationContext(),R.layout.list_item_result,result);
		setListAdapter(rla);
		
		int n = 0;
		for(Candidate c: result){
			n+=c.getVotes();
		}
		TextView tv = (TextView) this.findViewById(R.id.nbr_voters);
		tv.setText(getString(R.string.nbr_voters, n));
	}

}
