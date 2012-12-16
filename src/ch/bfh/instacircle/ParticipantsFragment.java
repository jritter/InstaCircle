package ch.bfh.instacircle;

import ch.bfh.instacircle.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ParticipantsFragment extends Fragment implements
		ParticipantsListFragment.Callbacks {
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_participants, container, false);
        return rootView;
    }

	public void onItemSelected(String id) {
		
		
	}
}
