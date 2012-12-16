package ch.bfh.adhocnetwork;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ParticipantCursorAdapter extends CursorAdapter {
	
	public ParticipantCursorAdapter(Context context, Cursor c) {
		super(context, c, 2);
	}
 
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView content = (TextView)view.findViewById(R.id.content);
		TextView description = (TextView)view.findViewById(R.id.description);
		ImageView icon = (ImageView)view.findViewById(R.id.icon);
		
		content.setText(cursor.getString(
				cursor.getColumnIndex("identification")));
        
		description.setText("\n");
	}
 
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.list_item_participant, parent, false);
		bindView(v, context, cursor);
		return v;
	}
}
