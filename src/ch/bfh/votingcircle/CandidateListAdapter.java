
package ch.bfh.votingcircle;

import java.util.List;

import ch.bfh.instacircle.R;
import ch.bfh.votingcircle.entities.Candidate;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;

/**
 * List adapter to show a list of candidates
 * @author von Bergen Philémon
 */
public class CandidateListAdapter extends ArrayAdapter<Candidate> {

	private Context context;
	private List<Candidate> values;

	public CandidateListAdapter(Context context,
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
			view =  inflater.inflate(R.layout.list_item_candidate, parent, false);
		} else {
			view = convertView;
		}
		final int position2 = position;

		//Text field
		EditText editText =  (EditText)view.findViewById(R.id.candidate);
		editText.setText(this.values.get(position).getText());
		editText.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus){
					final EditText et = (EditText) v;
					values.get(position2).setText(et.getText().toString());
				}
			}
		});

		//Delete button
		final CandidateListAdapter cla = this;
		ImageView cross = (ImageView)view.findViewById(R.id.delete_candidate);
		cross.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				values.remove(position2);
				cla.notifyDataSetChanged();
			}
		});
		return view;
	}


}
