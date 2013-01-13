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
 * a query to the participant table into the list item
 * 
 * @author Juerg Ritter (rittj1@bfh.ch)
 */
public class ParticipantCursorAdapter extends CursorAdapter {

	/**
	 * @param context
	 *            The context from which it has been created
	 * @param c
	 *            The cursor which should be mapped into the the view
	 */
	public ParticipantCursorAdapter(Context context, Cursor c) {
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
		content.setText(cursor.getString(cursor
				.getColumnIndex("identification")));

		// setting the icon according to the participant's state
		switch (cursor.getInt(cursor.getColumnIndex("state"))) {
		case 0:
			description.setText("inactive participant");
			icon.setBackgroundColor(context.getResources().getColor(
					android.R.color.holo_orange_light));
			break;
		case 1:
			description.setText("active participant");
			icon.setBackgroundColor(context.getResources().getColor(
					android.R.color.holo_green_light));
			break;
		default:
			description.setText("participant with unknown state");
			icon.setBackgroundColor(context.getResources().getColor(
					android.R.color.holo_red_light));
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
		View v = inflater
				.inflate(R.layout.list_item_participant, parent, false);
		bindView(v, context, cursor);
		return v;
	}
}
