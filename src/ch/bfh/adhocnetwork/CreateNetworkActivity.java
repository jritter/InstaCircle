package ch.bfh.adhocnetwork;

import ch.bfh.adhocnetwork.wifi.WifiAPManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

public class CreateNetworkActivity extends Activity implements OnClickListener {

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
		
		if (view == btnCreateNetwork){
			Toast.makeText(this, "Toggle WiFi AP", Toast.LENGTH_SHORT).show();
			
			WifiConfiguration wificonfig = new WifiConfiguration();
			wificonfig.SSID = txtNetworkName.getText().toString();
			wificonfig.preSharedKey = txtNetworkPIN.getText().toString();
			wificonfig.hiddenSSID = false;
			wificonfig.status = WifiConfiguration.Status.ENABLED;        
			wificonfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			wificonfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			wificonfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			wificonfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			wificonfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			wificonfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			
	    	wifiapman.enableHotspot(wifiman, wificonfig, this);
	    	
		}
		
	}

}
