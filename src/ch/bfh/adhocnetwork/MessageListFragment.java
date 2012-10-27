package ch.bfh.adhocnetwork;

import java.util.ArrayList;

import ch.bfh.adhocnetwork.db.NetworkDbHelper;
import ch.bfh.adhocnetwork.dummy.DummyContent;
import ch.bfh.adhocnetwork.service.NetworkService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MessageListFragment extends ListFragment {

	private static final String TAG = NetworkService.class.getSimpleName();
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    private Callbacks mCallbacks = sDummyCallbacks;
    private int mActivatedPosition = ListView.INVALID_POSITION;
    
    private ArrayList<Message> messages = new ArrayList<Message>();
    private SimpleCursorAdapter sca;
    
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
        
//        Message msg = new Message("ajlsdkjfk", 1, 2);
//        
//        messages.add(msg);
//        
//        setListAdapter(new ArrayAdapter<Message>(getActivity(),
//                R.layout.list_item_message,
//                R.id.label,
//                messages));
//        
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
				mMessageReceiver, new IntentFilter("messagesChanged"));
        
        helper = new NetworkDbHelper(getActivity());
        cursor = helper.queryMessages();
        
        
        sca = new SimpleCursorAdapter(getActivity(), R.layout.list_item_message, cursor, new String [] { "message" }, new int [] { R.id.label}, 2);
        
        setListAdapter(sca);
        
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
//        if (!(activity instanceof Callbacks)) {
//            throw new IllegalStateException("Activity must implement fragment's callbacks.");
//        }
//
//        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        mCallbacks.onItemSelected(DummyContent.ITEMS.get(position).id);
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
			sca.changeCursor(helper.queryMessages());
		}
	};
}
