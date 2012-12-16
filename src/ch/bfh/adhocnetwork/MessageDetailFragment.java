package ch.bfh.adhocnetwork;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MessageDetailFragment extends Fragment {

    public static final String ARG_ITEM_ID = "item_id";


    public MessageDetailFragment() {
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_message_detail, container, false);
        return rootView;
    }
}
