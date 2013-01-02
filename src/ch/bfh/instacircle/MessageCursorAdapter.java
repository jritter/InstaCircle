package ch.bfh.instacircle;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class MessageCursorAdapter extends CursorAdapter {

	public MessageCursorAdapter(Context context, Cursor c) {
		super(context, c, 2);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView content = (TextView) view.findViewById(R.id.content);
		TextView description = (TextView) view.findViewById(R.id.description);
		ImageView icon = (ImageView) view.findViewById(R.id.icon);

		content.setText(cursor.getString(cursor.getColumnIndex("message")));

		SimpleDateFormat sdf = new SimpleDateFormat();

		Date date = new Date(cursor.getLong(cursor.getColumnIndex("timestamp")));

		int messageType = cursor.getInt(cursor.getColumnIndex("message_type"));

		switch (messageType) {
		case Message.MSG_CONTENT:

			if (cursor.getInt(cursor.getColumnIndex("sequence_number")) == -1) {
				description.setText("Unicast message received from "
						+ cursor.getString(cursor
								.getColumnIndex("identification")) + ",\n"
						+ sdf.format(date));
				icon.setImageResource(R.drawable.glyphicons_120_message_full);
				icon.setBackgroundColor(context.getResources().getColor(
						android.R.color.holo_purple));
			} else {
				description.setText("Broadcast message received from "
						+ cursor.getString(cursor
								.getColumnIndex("identification")) + ",\n"
						+ sdf.format(date));
				icon.setImageResource(R.drawable.glyphicons_120_message_full);
				icon.setBackgroundColor(context.getResources().getColor(
						android.R.color.holo_blue_light));
			}
			break;
		case Message.MSG_MSGJOIN:
			description.setText("Participant joined,\n" + sdf.format(date));
			icon.setImageResource(R.drawable.glyphicons_006_user_add);
			break;
		case Message.MSG_MSGLEAVE:
			description.setText("Participant left,\n" + sdf.format(date));
			icon.setImageResource(R.drawable.glyphicons_007_user_remove);
			break;
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.list_item_message, parent, false);
		bindView(v, context, cursor);
		return v;
	}
}
