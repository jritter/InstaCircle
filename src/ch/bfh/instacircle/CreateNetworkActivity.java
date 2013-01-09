package ch.bfh.instacircle;

import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import ch.bfh.instacircle.service.NetworkService;
import ch.bfh.instacircle.wifi.WifiAPManager;

public class CreateNetworkActivity extends Activity implements OnClickListener,
		TextWatcher {

	private static final String TAG = CreateNetworkActivity.class
			.getSimpleName();

	private WifiAPManager wifiapman;
	private WifiManager wifiman;
	private Button btnCreateNetwork;

	private EditText txtNetworkName;
	private EditText txtNetworkPIN;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_network);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		txtNetworkName = (EditText) findViewById(R.id.network_name);
		txtNetworkPIN = (EditText) findViewById(R.id.network_pin);

		btnCreateNetwork = (Button) findViewById(R.id.create_network_button);
		btnCreateNetwork.setOnClickListener(this);

		wifiapman = new WifiAPManager();
		wifiman = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		WifiConfiguration config = wifiapman.getWifiApConfiguration(wifiman);
		txtNetworkName.setText(config.SSID);
		txtNetworkPIN.addTextChangedListener(this);
		txtNetworkPIN.setText(config.preSharedKey);

		if (config.preSharedKey == null || config.preSharedKey.length() < 8) {
			txtNetworkPIN.setText(UUID.randomUUID().toString().substring(0, 8));
		}

		if (wifiapman.isWifiAPEnabled(wifiman)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("WiFi AP");
			builder.setMessage("The Wifi AP already enabled. Use this connection?");
			builder.setPositiveButton("Yes",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							Intent intent = new Intent(
									CreateNetworkActivity.this,
									NetworkService.class);
							intent.putExtra("action", "createnetwork");
							stopService(intent);
							startService(intent);

							CreateNetworkActivity.this.finish();
						}
					});
			builder.setNegativeButton("No",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							return;
						}
					});
			AlertDialog dialog = builder.create();
			dialog.show();
		}

		if (txtNetworkPIN.getText().toString().length() < 8) {
			btnCreateNetwork.setEnabled(false);
		} else {
			btnCreateNetwork.setEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_create_network, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onClick(View view) {

		if (view == btnCreateNetwork) {

			if (wifiapman.isWifiAPEnabled(wifiman)) {
				wifiapman.disableHotspot(wifiman, this);
			}

			WifiConfiguration wificonfig = new WifiConfiguration();
			wificonfig.SSID = txtNetworkName.getText().toString();
			wificonfig.preSharedKey = txtNetworkPIN.getText().toString();
			wificonfig.hiddenSSID = false;
			wificonfig.status = WifiConfiguration.Status.ENABLED;
			// wificonfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
			// wificonfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
			wificonfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.TKIP);
			wificonfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.CCMP);
			wificonfig.allowedKeyManagement
					.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			wificonfig.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.TKIP);
			wificonfig.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.CCMP);
			wificonfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

			wifiapman.enableHotspot(wifiman, wificonfig, this);
		}
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
		
	}

	public void afterTextChanged(Editable s) {
		if (txtNetworkPIN.getText().toString().length() < 8) {
			btnCreateNetwork.setEnabled(false);
		} else {
			btnCreateNetwork.setEnabled(true);
		}
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {

	}
}
