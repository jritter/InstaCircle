package ch.bfh.instacircle;


import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ch.bfh.instacircle.R;
import ch.bfh.instacircle.db.NetworkDbHelper;

public class NetworkActiveActivity extends FragmentActivity implements ActionBar.TabListener {

	
	private static final String TAG = NetworkActiveActivity.class.getSimpleName();
	private static final String PREFS_NAME = "network_preferences";
	
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
     * sections. We use a {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will
     * keep every loaded fragment in memory. If this becomes too memory intensive, it may be best
     * to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    
    
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_network_active);
        // Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding tab.
        // We can also use ActionBar.Tab#select() to do this if we have a reference to the
        // Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by the adapter.
            // Also specify this Activity object, which implements the TabListener interface, as the
            // listener for when this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }

    

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		Intent intent = null;
		switch (item.getItemId()) {
		case R.id.display_qrcode:
			
			SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
			String serializedConfig = preferences.getString("SSID", "N/A") + "||" + preferences.getString("password", "N/A");
			
			intent = new Intent("com.google.zxing.client.android.ENCODE");
			intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
			intent.putExtra("ENCODE_DATA", serializedConfig);
			intent.putExtra("ENCODE_FORMAT", "QR_CODE");
			intent.putExtra("ENCODE_SHOW_CONTENTS", false);
			startActivity(intent);

			return true;
			
		case R.id.leave_network:
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Leave Network?");
            builder.setMessage("Do you really want to leave this conversation?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                	String identification = getSharedPreferences(PREFS_NAME, 0).getString("identification", "N/A");
                	String networkUUID = getSharedPreferences(PREFS_NAME, 0).getString("networkUUID", "N/A");
        			Message message = new Message(identification, Message.MSG_MSGLEAVE, identification, new NetworkDbHelper(NetworkActiveActivity.this).getNextSequenceNumber(), networkUUID);
        			Intent intent = new Intent("messageSend");
        			intent.putExtra("message", message);
        			LocalBroadcastManager.getInstance(NetworkActiveActivity.this).sendBroadcast(intent);
                } });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                	return;
                } });
            AlertDialog dialog = builder.create();
            dialog.show();
            
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
        	Fragment fragment = null; 
        	switch (i){
        	case 0:
        		fragment = new MessageFragment();
        		break;
        	case 1: 
//        		fragment = new DummySectionFragment();
//        		Bundle args = new Bundle();
//        		args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, i + 1);
//                fragment.setArguments(args);
                fragment = new ParticipantsFragment();
        		break;
        	}
            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return getString(R.string.tab_title_messages).toUpperCase();
                case 1: return getString(R.string.tab_title_participants).toUpperCase();
            }
            return null;
        }
    }

    /**
     * A dummy fragment representing a section of the app, but that simply displays dummy text.
     */
    public static class DummySectionFragment extends Fragment {
        public DummySectionFragment() {
        }

        public static final String ARG_SECTION_NUMBER = "section_number";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            TextView textView = new TextView(getActivity());
            textView.setGravity(Gravity.CENTER);
            Bundle args = getArguments();
            textView.setText(Integer.toString(args.getInt(ARG_SECTION_NUMBER)));
            return textView;
        }
    }
}
