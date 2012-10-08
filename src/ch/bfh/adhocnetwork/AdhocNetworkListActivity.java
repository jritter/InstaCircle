package ch.bfh.adhocnetwork;

import ch.bfh.adhocnetwork.R;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class AdhocNetworkListActivity extends FragmentActivity implements
		AdhocNetworkListFragment.Callbacks {

	private boolean mTwoPane;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_adhocnetwork_list);

		if (findViewById(R.id.adhocnetwork_detail_container) != null) {
			mTwoPane = true;
			((AdhocNetworkListFragment) getSupportFragmentManager()
					.findFragmentById(R.id.adhocnetwork_list))
					.setActivateOnItemClick(true);
		}

		

		Intent intent = new Intent(this, AdhocNetworkService.class);
		startService(intent);
	}

	public void onItemSelected(String id) {
		if (mTwoPane) {
			Bundle arguments = new Bundle();
			arguments.putString(AdhocNetworkDetailFragment.ARG_ITEM_ID, id);
			AdhocNetworkDetailFragment fragment = new AdhocNetworkDetailFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.adhocnetwork_detail_container, fragment)
					.commit();

		} else {
			Intent detailIntent = new Intent(this,
					AdhocNetworkDetailActivity.class);
			detailIntent.putExtra(AdhocNetworkDetailFragment.ARG_ITEM_ID, id);
			startActivity(detailIntent);
		}
	}
	
	public void onSendButtonClicked(View view) {
		
		EditText et = (EditText)findViewById(R.id.message_text);
		
		Toast.makeText(this, et.getText().toString(), Toast.LENGTH_SHORT).show();
		Message message = new Message(et.getText().toString(), 1, 1);
		Intent intent = new Intent("messageSend");
		intent.putExtra("message", message);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		et.setText("");
	}
}
