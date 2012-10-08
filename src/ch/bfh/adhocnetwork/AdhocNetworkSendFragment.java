package ch.bfh.adhocnetwork;

import ch.bfh.adhocnetwork.R;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AdhocNetworkSendFragment extends Fragment {

    public AdhocNetworkSendFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_adhocnetwork_send, container, false);
        return rootView;
    }
}
