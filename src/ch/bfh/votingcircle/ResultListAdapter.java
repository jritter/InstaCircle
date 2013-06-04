
package ch.bfh.votingcircle;

import java.util.List;

import ch.bfh.instacircle.R;
import ch.bfh.votingcircle.entities.Candidate;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * List adapter to show the result as a list
 * @author von Bergen Philémon
 */
public class ResultListAdapter extends ArrayAdapter<Candidate> {

	private Context context;
	private List<Candidate> values;

	public ResultListAdapter(Context context,
			int textViewResourceId, List<Candidate> objects) {
		super(context, textViewResourceId, objects);
		this.context=context;
		this.values=objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);

		View view;
		if (null == convertView) {
			view =  inflater.inflate(R.layout.list_item_result, parent, false);
		} else {
			view = convertView;
		}

		TextView resultCandidate =  (TextView)view.findViewById(R.id.result_candidate);
		resultCandidate.setText(this.values.get(position).getText());
		
		TextView resultVotes =  (TextView)view.findViewById(R.id.result_votes);
		String text = ""+this.values.get(position).getVotes();
		resultVotes.setText(text);
		
		return view;
	}


}
