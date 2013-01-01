package ch.bfh.instacircle;


import ch.bfh.instacircle.db.NetworkDbHelper;
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

public class MessageListFragment extends ListFragment {

	private static final String TAG = MessageListFragment.class.getSimpleName();
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private static final String PREFS_NAME = "network_preferences";

    private int mActivatedPosition = ListView.INVALID_POSITION;
    
    //private SimpleCursorAdapter sca;
    private MessageCursorAdapter mca;
    
    private Cursor cursor;
    private NetworkDbHelper helper;

    public interface Callbacks {
        public void onItemSelected(String id);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        public void onItemSelected(String id) {
        }
    };

    public MessageListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
				mMessageReceiver, new IntentFilter("messageArrived"));
        helper = new NetworkDbHelper(getActivity());
        cursor = helper.queryMessages();
        
        mca = new MessageCursorAdapter(getActivity(), cursor);
        
        setListAdapter(mca);
        
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
			mca.changeCursor(helper.queryMessages());
		}
	};

	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();
	}
	
	
	
	
}
