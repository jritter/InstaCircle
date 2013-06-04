package ch.bfh.votingcircle;

import java.io.Serializable;
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
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * Android activity allowing to define the parameters of the poll
 * @author Philémon von Bergen
 *
 */
public class ElectionInfoActivity extends ListActivity {

	CandidateListAdapter cla;
	EditText question;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_election_info);
		
		DataManager dm = ((EvotingApplication)this.getApplication()).getDataManager();
		dm.setElectionInfoActivity(this);
				
		String questionText = this.getIntent().getStringExtra("question");
		
		// initializing the adapter
		@SuppressWarnings("unchecked")
		List<Candidate> list = (List<Candidate>)this.getIntent().getSerializableExtra("candidates");
		
		cla = new CandidateListAdapter(this.getApplicationContext(),R.layout.list_item_candidate,list);
		setListAdapter(cla);
		
		Button b_save_start = (Button) this.findViewById(R.id.start_button);
		Button b_cancel = (Button) this.findViewById(R.id.cancel_button);
		ImageView b_add = (ImageView) this.findViewById(R.id.add_candidate);
		question = (EditText)this.findViewById(R.id.question);
		if(questionText!=null && !questionText.equals("")){
			question.setText(questionText);
		}
		b_save_start.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				if(cla.getCount()<2){
					Toast.makeText(getApplicationContext(), R.string.number_possible_response, Toast.LENGTH_LONG).show();
					return;
				}
				if(question.getText().toString().equals("")){
					Toast.makeText(getApplicationContext(), R.string.question_empty, Toast.LENGTH_LONG).show();
					return;
				}
				List<Candidate> list = new ArrayList<Candidate>();
				for(int i=0; i<cla.getCount(); i++){
					if(cla.getItem(i).getText().equals("")){
						Toast.makeText(getApplicationContext(), getString(R.string.possible_response_empty, i+1), Toast.LENGTH_LONG).show();
						return;
					}
					list.add(cla.getItem(i));
				}
				Intent resultIntent = new Intent();
				resultIntent.putExtra("question", question.getText().toString());
				resultIntent.putExtra("candidates", (Serializable)list);
				setResult(Activity.RESULT_OK,resultIntent);
				finish();
				
			}
			
		});
		
		b_cancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent resultIntent = new Intent();
				setResult(Activity.RESULT_CANCELED,resultIntent);
				finish();
			}
		});
		
		b_add.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				cla.add(new Candidate("",null));
				cla.notifyDataSetChanged();
			}
		});
	}

}
