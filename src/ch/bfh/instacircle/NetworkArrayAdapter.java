package ch.bfh.instacircle;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class NetworkArrayAdapter extends ArrayAdapter<HashMap<String, Object>> {
	
	private ArrayList<HashMap<String, Object>> items;
    private Context context;
	private String capabilities;
    
	public NetworkArrayAdapter(Context context, int textViewResourceId, ArrayList<HashMap<String, Object>> items) {
        super(context, textViewResourceId, items);
        this.context = context;
        this.items = items;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_item_network, null);
        }

        HashMap<String, Object> item = items.get(position);
        if (item!= null) {
        	TextView content = (TextView) view.findViewById(R.id.content);
    		TextView description = (TextView) view.findViewById(R.id.description);
    		ImageView icon = (ImageView) view.findViewById(R.id.icon);
            
    		if (this.getCount() - 1 == position){
    			icon.setImageResource(R.drawable.glyphicons_046_router);
    			icon.setBackgroundColor(context.getResources().getColor(
						android.R.color.holo_purple));
    			description.setText("");
    		} else {
    			icon.setImageResource(R.drawable.glyphicons_032_wifi_alt);
    			icon.setBackgroundColor(context.getResources().getColor(
						android.R.color.holo_blue_light));
    			capabilities = (String) item.get("capabilities");
	    		if (capabilities == null){
	    			description.setText("");
	    		} else if (capabilities.contains("WPA")) {
	    			description.setText("WPA secured network");
				} else if (capabilities.contains("WEP")) {
					description.setText("WEP secured network");
				}  
				else {
					description.setText("unsecure open network");
				}
    		}
    		content.setText((String) item.get("SSID"));
    		
         }
        
        

        return view;
    }
}
