
package ch.bfh.votingcircle;

import java.util.List;

import ch.bfh.instacircle.R;
import ch.bfh.votingcircle.entities.Participant;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * List adapter for the list of participants
 * @author von Bergen Philémon
 */
public class ParticipantListAdapter extends ArrayAdapter<Participant> {

	private Context context;
	private List<Participant> values;

	public ParticipantListAdapter(Context context,
			int textViewResourceId, List<Participant> objects) {
		super(context, textViewResourceId, objects);
		this.context=context;
		this.values=objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);

		View view;
		if (null == convertView) {
			view =  inflater.inflate(R.layout.list_item_participant, parent, false);
		} else {
			view = convertView;
		}

		// extracting the labels of the layout
		TextView content = (TextView) view.findViewById(R.id.content);
		TextView description = (TextView) view.findViewById(R.id.description);
		ImageView icon = (ImageView) view.findViewById(R.id.icon);

		// setting the content label
		content.setText(values.get(position).getIdentification());

		int state = values.get(position).getState();

		// setting the icon according to the participant's state
		switch (state) {
		
		case 0:
			description.setText("Message not received");
			icon.setBackgroundColor(context.getResources().getColor(
					android.R.color.holo_orange_light));
			break;
		case 1:
			description.setText("Message received");
			icon.setBackgroundColor(context.getResources().getColor(
					android.R.color.holo_green_light));
			break;

		default:
			description.setText("Participant excluded");
			icon.setBackgroundColor(context.getResources().getColor(
					android.R.color.holo_red_light));
			break;
		}

		return view;
	}
	
}
