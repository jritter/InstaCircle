/*
 *  UniCrypt Cryptographic Library
 *  Copyright (c) 2013 Berner Fachhochschule, Biel, Switzerland.
 *  All rights reserved.
 *
 *  Distributable under GPL license.
 *  See terms of license at gnu.org.
 *  
 */

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

/**
 * This class implements a CursorAdapter which maps the fields of the cursor of
 * a Query to the message table into the list item
 * 
 * @author Juerg Ritter (rittj1@bfh.ch)
 */
public class MessageCursorAdapter extends CursorAdapter {

	/**
	 * @param context
	 *            The context from which it has been created
	 * @param c
	 *            The cursor which should be mapped into the the view
	 */
	public MessageCursorAdapter(Context context, Cursor c) {
		super(context, c, 2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.widget.CursorAdapter#bindView(android.view.View,
	 * android.content.Context, android.database.Cursor)
	 */
	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		// extracting the labels of the layout
		TextView content = (TextView) view.findViewById(R.id.content);
		TextView description = (TextView) view.findViewById(R.id.description);
		ImageView icon = (ImageView) view.findViewById(R.id.icon);

		// setting the content label
		content.setText(cursor.getString(cursor.getColumnIndex("message")));

		SimpleDateFormat sdf = new SimpleDateFormat();

		// create a readable date from the timestamp
		Date date = new Date(cursor.getLong(cursor.getColumnIndex("timestamp")));

		int messageType = cursor.getInt(cursor.getColumnIndex("message_type"));

		// setting the icon according to the message type
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.widget.CursorAdapter#newView(android.content.Context,
	 * android.database.Cursor, android.view.ViewGroup)
	 */
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.list_item_message, parent, false);
		bindView(v, context, cursor);
		return v;
	}
}
