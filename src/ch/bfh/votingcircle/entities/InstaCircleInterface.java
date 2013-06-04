package ch.bfh.votingcircle.entities;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import org.apache.http.conn.util.InetAddressUtils;
import org.apache.log4j.Logger;

import ch.bfh.instacircle.Message;
import ch.bfh.instacircle.db.NetworkDbHelper;
import ch.bfh.instacircle.wifi.WifiAPManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Class representing the interface between InstaCircle and the voting application
 * @author Philémon von Bergen
 *
 */
public class InstaCircleInterface {

	private Logger logger = DataManager.LOGGER;
	private NetworkDbHelper ndbh;
	private DataManager dm;

	public InstaCircleInterface(DataManager dm) {

		this.dm = dm;

		//Register to Android broadcasts
		LocalBroadcastManager.getInstance(dm.getContext()).registerReceiver(
				this.countParticipantsRequestReceiver, new IntentFilter("countParticipants"));

		LocalBroadcastManager.getInstance(dm.getContext()).registerReceiver(
				this.messageSendRequestReceiver, new IntentFilter("sendMessage"));

		LocalBroadcastManager.getInstance(dm.getContext()).registerReceiver(
				this.resetRequestReceiver, new IntentFilter("resetInterface"));

		// Subscribing to the participantJoined and participantChangedState events
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("participantJoined");
		intentFilter.addAction("participantChangedState");
		LocalBroadcastManager.getInstance(dm.getContext()).registerReceiver(participantsDiscoverer, intentFilter);


		//Some initializations
		ndbh = NetworkDbHelper.getInstance(dm.getContext());		


		String identification = dm.getContext().getSharedPreferences("network_preferences", 0)
				.getString("identification", "N/A");
		dm.setMyIdentification(identification);

	}

	/**
	 * Get the messages that have to be sent to instaCircle and send them 
	 */
	protected BroadcastReceiver messageSendRequestReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			//Get the message
			String messageContent = intent.getStringExtra("messageContent");
			if(messageContent==null) messageContent="";
			int type = intent.getIntExtra("type",0);
			String ipAddress = intent.getStringExtra("ipAddress");

			//Send it to instaCircle
			try{
				//Create the message
				Message messageToSend = new Message(messageContent, type, dm.getMyIdentification(),	ndbh.getNextSequenceNumber());

				//Send message as broadcast
				if(ipAddress==null){
					Intent intentToSend = new Intent("messageSend");
					intentToSend.putExtra("message", messageToSend);
					intentToSend.putExtra("broadcast", true);

					LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intentToSend);
					logger.debug(this.getClass().getSimpleName()+" Broadcast sent");
				}
				//Send message as unicast
				else{
					Intent intentToSend = new Intent("messageSend");
					intentToSend.putExtra("message", messageToSend);
					intentToSend.putExtra("ipAddress", ipAddress);
					intentToSend.putExtra("broadcast", false);

					LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intentToSend);
					logger.debug(this.getClass().getSimpleName()+" Unicast sent to "+ipAddress);
				}
			} catch (Exception e){
				logger.error("Error while sending message: "+e.getMessage());
			}
		}
	};

	/**
	 * Remember the number of participants the last time that this method was
	 * called (request sent from EvotingCircle)
	 */
	protected BroadcastReceiver countParticipantsRequestReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			//Get participants to the protocol
			HashMap<String,Participant> participantsMap = new HashMap<String,Participant>();
			List<Participant> list = getDiscussionParticipants();

			//Create a map with the participants
			for(Participant p:list){
				participantsMap.put(p.getIpAddress(), p);
			}
			dm.setTempParticipants(participantsMap);

			Intent intentUpdate = new Intent("participantMessageReceived");
			LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intentUpdate);
		}
	};

	/**
	 * if there is the new participant or the state of a participant has been
	 * changed (Info sent from InstaCircle)
	 */
	private BroadcastReceiver participantsDiscoverer = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(!dm.isProtocolStarted() && dm.isActive()){
				List<Participant> list = getDiscussionParticipants();


				HashMap<String,Participant> participantsMap = new HashMap<String,Participant>();

				//Create a map with the participants
				for(Participant p:list){
					participantsMap.put(p.getIpAddress(), p);
				}
				dm.setTempParticipants(participantsMap);

				Intent intentUpdate = new Intent("participantMessageReceived");
				LocalBroadcastManager.getInstance(dm.getContext()).sendBroadcast(intentUpdate);
			}
		}
	};


	/**
	 * Unregister the LocalBroadcastReceivers
	 */
	protected BroadcastReceiver resetRequestReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			LocalBroadcastManager.getInstance(dm.getContext()).unregisterReceiver(countParticipantsRequestReceiver);
			LocalBroadcastManager.getInstance(dm.getContext()).unregisterReceiver(messageSendRequestReceiver);
			LocalBroadcastManager.getInstance(dm.getContext()).unregisterReceiver(participantsDiscoverer);
			LocalBroadcastManager.getInstance(dm.getContext()).unregisterReceiver(resetRequestReceiver);
		}
	};

	/**
	 * Get the actual participant in the network
	 * @return a list of participants
	 */
	private List<Participant> getDiscussionParticipants(){
		List<Participant> list = new ArrayList<Participant>();
		try{
			Cursor c = ndbh.queryParticipants();
			c.moveToFirst();
			while(!c.isAfterLast()){
				String ipAddress = c.getString(c.getColumnIndex("ip_address"));
				String identification = c.getString(c.getColumnIndex("identification"));
				list.add(new Participant(ipAddress,identification));
				c.moveToNext();
			}
			c.close();
		} catch (Exception e){
			logger.error("Error occured while retrieving participants: "+e.getMessage());
		}

		dm.setMyIpAddress(this.getIPAddress(true));
		logger.debug("InstaCircleInterface: my ip address is "+dm.getMyIpAddress());

		return list;
	}


	/**
	 * Get IP address from first non-localhost interface
	 * @param ipv4  true=return ipv4, false=return ipv6
	 * @return  address or empty string
	 * @autor http://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device
	 */
	private String getIPAddress(boolean useIPv4) {
		/*try {
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
				for (InetAddress addr : addrs) {
					if (!addr.isLoopbackAddress()) {
						String sAddr = addr.getHostAddress().toUpperCase();
						boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr); 
						if (useIPv4) {
							if (isIPv4) 
								return sAddr;
						} else {
							if (!isIPv4) {
								int delim = sAddr.indexOf('%'); // drop ip6 port suffix
								return delim<0 ? sAddr : sAddr.substring(0, delim);
							}
						}
					}
				}
			}
		} catch (Exception ex) { 
			//if exception occurred return null
		}
		return null;*/

		WifiManager wifiManager = (WifiManager) this.dm.getContext().getSystemService(Context.WIFI_SERVICE);
		String ipString = null;

		if(new WifiAPManager().isWifiAPEnabled(wifiManager)){

			try{
				for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
						.hasMoreElements();) {
					NetworkInterface intf = en.nextElement();
					if (intf.getName().contains("wlan")) {
						for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
								.hasMoreElements();) {
							InetAddress inetAddress = enumIpAddr.nextElement();
							if (!inetAddress.isLoopbackAddress()
									&& (inetAddress.getAddress().length == 4)) {
								ipString = inetAddress.getHostAddress();
								break;
							}
						}
					}
				}
			} catch (SocketException e){
				e.printStackTrace();
			}
		} else {

			WifiManager wifi = (WifiManager) this.dm.getContext().getSystemService(Context.WIFI_SERVICE);
			DhcpInfo dhcp = wifi.getDhcpInfo();

			InetAddress found_ip_address = null;
			int ip = dhcp.ipAddress;
			byte[] quads = new byte[4];
			for (int k = 0; k < 4; k++)
				quads[k] = (byte) (ip >> (k * 8));
			try {
				found_ip_address =  InetAddress.getByAddress(quads);
				ipString = found_ip_address.getHostAddress();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}


		/*WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();
		String ipString = String.format("%d.%d.%d.%d",(ip & 0xff),(ip >> 8 & 0xff),(ip >> 16 & 0xff),(ip >> 24 & 0xff));
		 */
		/**/

		return ipString;
	}

}
