package ch.bfh.instacircle;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;
import ch.bfh.instacircle.R;
import ch.bfh.instacircle.db.NetworkDbHelper;

public class ParticipantDetailFragment extends Fragment implements
		ParticipantsListFragment.Callbacks {
	
	private static final String TAG = ParticipantDetailFragment.class.getSimpleName();
	
	
	private GridLayout layout;
	private TextView participantIdentification;
	private TextView participantIpAddress;
	private TextView participantStatus;
	private TextView participantSequenceNumber;
	
	private int participantId;
	private NetworkDbHelper dbHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_participant_detail, container, false);
        return rootView;
    }

	public void onItemSelected(String id) {
		
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
				
		dbHelper = new NetworkDbHelper(getActivity());
        
        participantId = getActivity().getIntent().getIntExtra("participant_id", -1);
        
        
        layout = (GridLayout) getView().findViewById(R.id.participant_detail_layout);
        
        participantIdentification = (TextView) layout.findViewById(R.id.participant_identification);
        participantIpAddress = (TextView) layout.findViewById(R.id.participant_ip_address);
        participantStatus = (TextView) layout.findViewById(R.id.participant_status);
        participantSequenceNumber = (TextView) layout.findViewById(R.id.participant_sequence_number);
        
        
        Cursor participant = dbHelper.queryParticipant(participantId);
        participant.moveToFirst();
        
        participantIdentification.setText(participant.getString(participant.getColumnIndex("identification")));
        participantIpAddress.setText(participant.getString(participant.getColumnIndex("ip_address")));
        
        switch (participant.getInt(participant.getColumnIndex("state"))){
        case 0:
        	participantStatus.setTextColor(getActivity().getResources().getColor(android.R.color.holo_orange_light));
        	participantStatus.setText("inactive");
        	break;
        case 1:
        	participantStatus.setTextColor(getActivity().getResources().getColor(android.R.color.holo_green_light));
        	participantStatus.setText("active");
        	break;
        default:
        	participantStatus.setTextColor(getActivity().getResources().getColor(android.R.color.holo_red_light));
        	participantStatus.setText("unknown");
        	break;
        }
        
        participantSequenceNumber.setText("" + dbHelper.getCurrentParticipantSequenceNumber(participant.getString(participant.getColumnIndex("identification"))));
	}
}
