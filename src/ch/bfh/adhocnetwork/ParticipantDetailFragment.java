package ch.bfh.adhocnetwork;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;
import ch.bfh.adhocnetwork.db.NetworkDbHelper;

public class ParticipantDetailFragment extends Fragment implements
		ParticipantsListFragment.Callbacks {
	
	private static final String TAG = ParticipantDetailFragment.class.getSimpleName();
	
	
	private GridLayout layout;
	private TextView participantIdentification;
	private TextView participantIpAddress;
	private TextView participantStatus;
	
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
        
        
        Cursor participant = dbHelper.queryParticipant(participantId);
        participant.moveToFirst();
        
        participantIdentification.setText(participant.getString(participant.getColumnIndex("identification")));
        participantIpAddress.setText(participant.getString(participant.getColumnIndex("ip_address")));
        
        switch (participant.getInt(participant.getColumnIndex("state"))){
        case 0:
        	participantStatus.setText("inactive");
        	break;
        case 1:
        	participantStatus.setText("active");
        	break;
        default:
        	participantStatus.setText("unknown");
        	break;
        }
        
        
	}
}
