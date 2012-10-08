package ch.bfh.adhocnetwork;

import ch.bfh.adhocnetwork.R;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

public class AdhocNetworkDetailActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adhocnetwork_detail);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putString(AdhocNetworkDetailFragment.ARG_ITEM_ID,
                    getIntent().getStringExtra(AdhocNetworkDetailFragment.ARG_ITEM_ID));
            AdhocNetworkDetailFragment fragment = new AdhocNetworkDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.adhocnetwork_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpTo(this, new Intent(this, AdhocNetworkListActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
