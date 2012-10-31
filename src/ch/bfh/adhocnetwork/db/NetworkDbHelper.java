package ch.bfh.adhocnetwork.db;

import ch.bfh.adhocnetwork.Message;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class NetworkDbHelper extends SQLiteOpenHelper {

	private static final String TAG = NetworkDbHelper.class.getSimpleName();

	// Basic DB parameters
	private static final String DATABASE_NAME = "network.db";
	private static final int DATABASE_VERSION = 1;

	// Table names
	private static final String TABLE_NAME_MESSAGES = "messages";
	private static final String TABLE_NAME_PARTICIPANTS = "participants";

	// Attributes of the messages table
	private static final String MESSAGES_ID = "_id";
	private static final String MESSAGES_MESSAGE = "message";
	private static final String MESSAGES_MESSAGE_TYPE = "message_type";
	private static final String MESSAGES_SENDER_ID = "sender_id";
	private static final String MESSAGES_SEQUENCE_NUMBER = "sequence_number";
	private static final String MESSAGES_TIMESTAMP = "timestamp";

	// Attributes of the participants table
	private static final String PARTICIPANTS_ID = "_id";
	private static final String PARTICIPANTS_IDENTIFICATION = "identification";
	
	private String networkUUID; 

	public NetworkDbHelper(Context context, String networkUUID) {
		super(context, networkUUID + "_" + DATABASE_NAME, null, DATABASE_VERSION);
		this.networkUUID = networkUUID;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME_PARTICIPANTS + " ("
				+ PARTICIPANTS_ID + " INTEGER PRIMARY KEY, "
				+ PARTICIPANTS_IDENTIFICATION + " TEXT);");

		
		String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_MESSAGES + " ("
				+ MESSAGES_ID + " INTEGER PRIMARY KEY, "
				+ MESSAGES_MESSAGE + " TEXT, "
				+ MESSAGES_MESSAGE_TYPE	+ " INTEGER, " 
				+ MESSAGES_SENDER_ID + " INTEGER, "
				+ MESSAGES_SEQUENCE_NUMBER	+ " INTEGER, "
				+ MESSAGES_TIMESTAMP + " INTEGER, FOREIGN KEY(" + MESSAGES_SENDER_ID
				+ ") REFERENCES " + TABLE_NAME_PARTICIPANTS + "("
				+ PARTICIPANTS_ID + "));";
		
		
		Log.d(TAG, sql);
		db.execSQL(sql);
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "DB Upgrade from Version " + oldVersion + " to version "
				+ newVersion);
		db.execSQL("DROP TABLE IF EXISTS ?",
				new String[] { TABLE_NAME_PARTICIPANTS });
		db.execSQL("DROP TABLE IF EXISTS ?",
				new String[] { TABLE_NAME_MESSAGES });
		onCreate(db);
	}

	public void insertMessage(Message message) {
		long rowId = -1;
		try {
			SQLiteDatabase db = getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(MESSAGES_MESSAGE, message.getMessage());
			values.put(MESSAGES_MESSAGE_TYPE, message.getMessageType());
			values.put(MESSAGES_SENDER_ID, 1);
			values.put(MESSAGES_SEQUENCE_NUMBER, message.getSequenceNumber());
			values.put(MESSAGES_TIMESTAMP, System.currentTimeMillis());
			rowId = db.insert(TABLE_NAME_MESSAGES, null, values);
		} catch (SQLiteException e) {
			Log.e(TAG, "insert()", e);
		} finally {
			Log.d(TAG, "insert(): rowId=" + rowId);
		}
	}

	public void insertParticipants(String participantIdentification) {
		long rowId = -1;
		try {
			SQLiteDatabase db = getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(PARTICIPANTS_IDENTIFICATION, participantIdentification);
			rowId = db.insert(TABLE_NAME_PARTICIPANTS, null, values);
		} catch (SQLiteException e) {
			Log.e(TAG, "insert()", e);
		} finally {
			Log.d(TAG, "insert(): rowId=" + rowId);
		}
	}

	public Cursor queryMessages() {
		SQLiteDatabase db = getReadableDatabase();
		
		String query = "SELECT * FROM messages m LEFT OUTER JOIN participants p ON m.sender_id = p._id ORDER BY m.timestamp ASC;";
		Cursor c = db.rawQuery(query, null);
		
		return c;
	}

	public Cursor queryParticipants() {
		SQLiteDatabase db = getReadableDatabase();
		Cursor c =  db.query(TABLE_NAME_PARTICIPANTS, null, null, null, null, null,
				PARTICIPANTS_IDENTIFICATION + " ASC");
		return c;
	}
}
