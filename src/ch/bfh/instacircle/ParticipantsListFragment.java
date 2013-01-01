package ch.bfh.instacircle;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ListView;
import ch.bfh.instacircle.db.NetworkDbHelper;

public class ParticipantsListFragment extends ListFragment {

	private static final String TAG = ParticipantsListFragment.class.getSimpleName();
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private static final String PREFS_NAME = "network_preferences";

    private int mActivatedPosition = ListView.INVALID_POSITION;
    
    //private SimpleCursorAdapter sca;
    private ParticipantCursorAdapter pca;
    
    private Cursor cursor;
    private NetworkDbHelper helper;

    public interface Callbacks {
        public void onItemSelected(String id);
    }

    public ParticipantsListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
              
        
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("participantJoined");
        intentFilter.addAction("participantChangedState");
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
				mMessageReceiver, intentFilter);
		String networkUUID = getActivity().getSharedPreferences(PREFS_NAME, 0).getString("networkUUID", "");
        helper = new NetworkDbHelper(getActivity());
        cursor = helper.queryParticipants();
        
        pca = new ParticipantCursorAdapter(getActivity(), cursor);
        
        setListAdapter(pca);
        
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null && savedInstanceState
                .containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
        
        this.getListView().setTranscriptMode(2);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        Cursor cursor = (Cursor) getListAdapter().getItem(position);
        Intent intent = new Intent(getActivity(), ParticipantDetailActivity.class);
        intent.putExtra("participant_id", cursor.getInt(cursor.getColumnIndex("_id")));
        getActivity().startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    public void setActivateOnItemClick(boolean activateOnItemClick) {
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    public void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }
    
    @Override
	public void onDestroy() {
	  // Unregister since the activity is about to be closed.
	  LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
	  super.onDestroy();
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			pca.changeCursor(helper.queryParticipants());
		}
	};
	
	
}