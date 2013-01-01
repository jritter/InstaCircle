package ch.bfh.instacircle.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import ch.bfh.instacircle.Message;

public class NetworkDbHelper extends SQLiteOpenHelper {

	private static final String TAG = NetworkDbHelper.class.getSimpleName();
	private static final String PREFS_NAME = "network_preferences";

	// Basic DB parameters
	private static final String DATABASE_NAME = "network.db";
	private static final int DATABASE_VERSION = 8;

	// Table names
	private static final String TABLE_NAME_MESSAGE = "message";
	private static final String TABLE_NAME_PARTICIPANT = "participant";
	private static final String TABLE_NAME_CONVERSATION = "conversation";

	// Attributes of the messages table
	private static final String MESSAGE_ID = "_id";
	private static final String MESSAGE_MESSAGE = "message";
	private static final String MESSAGE_MESSAGE_TYPE = "message_type";
	private static final String MESSAGE_SENDER_ID = "sender_id";
	private static final String MESSAGES_SEQUENCE_NUMBER = "sequence_number";
	private static final String MESSAGES_SOURCE_IP_ADDRESS = "source_ip_address";
	private static final String MESSAGES_TIMESTAMP = "timestamp";

	// Attributes of the participants table
	private static final String PARTICIPANT_ID = "_id";
	private static final String PARTICIPANT_CONVERSATION_ID = "conversation_id";
	private static final String PARTICIPANT_IDENTIFICATION = "identification";
	private static final String PARTICIPANT_IP_ADDRESS = "ip_address";
	private static final String PARTICIPANT_STATE = "state";
	
	// Attributes of the conversations table
	private static final String CONVERSATION_ID = "_id";
	private static final String CONVERSATION_KEY = "conversation_key";
	private static final String CONVERSATION_UUID = "conversation_uuid";
	private static final String CONVERSATION_START = "conversation_start";
	private static final String CONVERSATION_END = "conversation_end";
	private static final String CONVERSATION_OPEN = "conversation_open";
	
	private Context context;
	private String identification;
	

	public NetworkDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
		identification = context.getSharedPreferences(PREFS_NAME, 0).getString("identification", "N/A");
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		
		String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_CONVERSATION + " ("
				+ CONVERSATION_ID + " INTEGER PRIMARY KEY, "
				+ CONVERSATION_KEY + " TEXT, "
				+ CONVERSATION_UUID + " TEXT, "
				+ CONVERSATION_START + " INTEGER, "
				+ CONVERSATION_END + " INTEGER, "
				+ CONVERSATION_OPEN + " INTEGER);";

		db.execSQL(sql);
		
		sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_PARTICIPANT + " ("
				+ PARTICIPANT_ID + " INTEGER PRIMARY KEY, "
				+ PARTICIPANT_CONVERSATION_ID + " INTEGER, "
				+ PARTICIPANT_IDENTIFICATION + " TEXT, "
				+ PARTICIPANT_IP_ADDRESS + " TEXT, "
				+ PARTICIPANT_STATE + " INTEGER, "
				+ "FOREIGN KEY(" + PARTICIPANT_CONVERSATION_ID + ") REFERENCES " + TABLE_NAME_CONVERSATION + "(" + CONVERSATION_ID + "));";
		
		db.execSQL(sql);
		
		
		
		sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_MESSAGE + " ("
				+ MESSAGE_ID + " INTEGER PRIMARY KEY, "
				+ MESSAGE_MESSAGE + " TEXT, "
				+ MESSAGE_MESSAGE_TYPE	+ " INTEGER, " 
				+ MESSAGE_SENDER_ID + " INTEGER, "
				+ MESSAGES_SEQUENCE_NUMBER	+ " INTEGER, "
				+ MESSAGES_SOURCE_IP_ADDRESS + " TEXT, "
				+ MESSAGES_TIMESTAMP + " INTEGER, "
				+ "FOREIGN KEY(" + MESSAGE_SENDER_ID + ") REFERENCES " + TABLE_NAME_PARTICIPANT + "("	+ PARTICIPANT_ID + "));";
		
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "DB Upgrade from Version " + oldVersion + " to version "
				+ newVersion);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_CONVERSATION);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_PARTICIPANT);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_MESSAGE);
		onCreate(db);
	}

	public void insertMessage(Message message) {
		long rowId = -1;
		Log.d(TAG, "Inserting Message: " + message.toString());
		try {
			SQLiteDatabase db = getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(MESSAGE_MESSAGE, message.getMessage());
			values.put(MESSAGE_MESSAGE_TYPE, message.getMessageType());
			values.put(MESSAGE_SENDER_ID, getParticipantID(message.getSender()));
			values.put(MESSAGES_SEQUENCE_NUMBER, message.getSequenceNumber());
			values.put(MESSAGES_SOURCE_IP_ADDRESS, message.getSenderIPAddress());
			values.put(MESSAGES_TIMESTAMP, message.getTimestamp());
			rowId = db.insert(TABLE_NAME_MESSAGE, null, values);
		} catch (SQLiteException e) {
			Log.e(TAG, "insert()", e);
		} finally {
			Log.d(TAG, "insert(): rowId=" + rowId);
		}
	}
	
	public long getParticipantID(String participantIdentification, long conversationId){
		SQLiteDatabase db = getReadableDatabase();
		String sql= "SELECT * FROM participant p WHERE p.identification = '" + participantIdentification + "' AND p.conversation_id = " + conversationId;
		Cursor c = db.rawQuery(sql, null);
		c.moveToFirst();
		return c.getLong(c.getColumnIndex("_id"));
	}
	
	public long getParticipantID(String participantIdentification){
		return getParticipantID(participantIdentification, getOpenConversationId());
	}
	
	public Cursor queryParticipant(int participantId){
		SQLiteDatabase db = getReadableDatabase();
		String sql = "SELECT * FROM participant p WHERE _id = " + participantId;
		Cursor c = db.rawQuery(sql, null);
		return c;
	}

	public long insertParticipant(String participantIdentification, String ipAddress, long conversationId) {
		long rowId = -1;
		try {
			SQLiteDatabase db = getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(PARTICIPANT_IDENTIFICATION, participantIdentification);
			values.put(PARTICIPANT_CONVERSATION_ID, conversationId);
			values.put(PARTICIPANT_IP_ADDRESS, ipAddress);
			values.put(PARTICIPANT_STATE, 1);
			rowId = db.insert(TABLE_NAME_PARTICIPANT, null, values);
		} catch (SQLiteException e) {
			Log.e(TAG, "insert()", e);
		} finally {
			Log.d(TAG, "insert(): rowId=" + rowId);
		}
		return rowId;
	}
	
	public void updateParticipantState(String participantIdentification, int newState){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(PARTICIPANT_STATE, newState);
		db.update(TABLE_NAME_PARTICIPANT, values, PARTICIPANT_IDENTIFICATION + " = '" + participantIdentification + "'", null);
	}
	
	public long insertParticipant(String participantIdentification, String ipAddress){
		return insertParticipant(participantIdentification, ipAddress, getOpenConversationId());
	}
	
	public Cursor queryMessages(long conversationId) {
		SQLiteDatabase db = getReadableDatabase();
		
		//String query = "SELECT * FROM messages m LEFT OUTER JOIN participants p ON m.sender_id = p._id ORDER BY m.timestamp ASC;";
		String sql = "SELECT * FROM participant p, conversation c, message m WHERE m.sender_id = p._id AND p.conversation_id = c._id AND c._id = " + conversationId + " ORDER BY timestamp ASC";
		Cursor c = db.rawQuery(sql, null);
		return c;
	}
	
	public Cursor queryMessages(){
		return queryMessages(getOpenConversationId());
	}
	
	public Cursor queryMyMessages (long conversationId) {
		SQLiteDatabase db = getReadableDatabase();
		String myIdentification = context.getSharedPreferences("network_preferences", 0).getString("identification", "N/A");
		String sql = "SELECT * FROM participant p, conversation c, message m WHERE m.sender_id = p._id AND p.conversation_id = c._id AND c._id = " + conversationId + " AND p.identification = '" + myIdentification + "' ORDER BY timestamp ASC;";
		Cursor c = db.rawQuery(sql, null);
		return c;
	}
	
	public Cursor queryMyMessages() {
		return queryMyMessages(getOpenConversationId());
	}

	public Cursor queryParticipants(long conversationId) {
		SQLiteDatabase db = getReadableDatabase();
		String sql = "SELECT p._id, p.identification, p.ip_address, p.state FROM participant p, conversation c WHERE p.conversation_id = c._id AND c._id = " + conversationId;
//		Cursor c =  db.query(TABLE_NAME_PARTICIPANT, null, null, null, null, null,
//				PARTICIPANT_IDENTIFICATION + " ASC");
		
		Cursor c = db.rawQuery(sql, null);
		return c;
	}
	
	public Cursor queryParticipants(){
		return queryParticipants(getOpenConversationId());
	}
	
	public long openConversation(String key, String UUID){	
		Log.d(TAG, "open Converation");
		
		long rowId = -1;
		
//		SQLiteDatabase db = getReadableDatabase();
//		String sql = "SELECT * FROM " + TABLE_NAME_CONVERSATION + " WHERE " + CONVERSATION_UUID + " = '" + UUID + "' AND " + CONVERSATION_OPEN + " = 1;";
//		
//		Cursor c = db.rawQuery(sql, null);
//		if (c.getCount() != 0){
//			c.moveToFirst();
//			rowId = c.getLong(c.getColumnIndex(CONVERSATION_ID));
//		}
//		else {
//			db = getWritableDatabase();
//			ContentValues values = new ContentValues();
//			values.put(CONVERSATION_KEY, key);
//			values.put(CONVERSATION_UUID, UUID);
//			values.put(CONVERSATION_START, System.currentTimeMillis());
//			values.put(CONVERSATION_OPEN, 1);
//			rowId = db.insert(TABLE_NAME_CONVERSATION, null, values);
//		}
//		c.close();
		
		closeConversation();
		
		SQLiteDatabase db = getReadableDatabase();
		String query = "SELECT * FROM " + TABLE_NAME_CONVERSATION + " WHERE " + CONVERSATION_UUID + " = '" + UUID + "'";
		Cursor c = db.rawQuery(query, null);
		if (c.getCount() == 0){
			db = getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(CONVERSATION_KEY, key);
			values.put(CONVERSATION_UUID, UUID);
			values.put(CONVERSATION_START, System.currentTimeMillis());
			values.put(CONVERSATION_OPEN, 1);
			rowId = db.insert(TABLE_NAME_CONVERSATION, null, values);
		}
		else {
			c.moveToLast();
			rowId = c.getLong(c.getColumnIndex(CONVERSATION_ID));
			ContentValues values = new ContentValues();
			values.put(CONVERSATION_OPEN, 1);
			db.update(TABLE_NAME_CONVERSATION, values, CONVERSATION_ID + " = " + rowId, null);
		}
		
		return rowId;
	}
	
	public void closeConversation(){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(CONVERSATION_OPEN, 0);
		values.put(CONVERSATION_END, System.currentTimeMillis());
		db.update(TABLE_NAME_CONVERSATION, values, CONVERSATION_OPEN + " = 1", null);
		//Log.d(TAG, "Conversation closed...");
	}
	
	public long getOpenConversationId(){
		SQLiteDatabase db = getReadableDatabase();
		String query = "SELECT * FROM " + TABLE_NAME_CONVERSATION + " WHERE " + CONVERSATION_OPEN + " = 1";
		Cursor c = db.rawQuery(query, null);
		if (c.getCount() == 0){
			return -1;
		}
		else {
			c.moveToFirst();
			long conversationId = c.getLong(c.getColumnIndex(CONVERSATION_ID));
			return conversationId;
		}
	}
	
	public String getOpenConversationUUID(){
		SQLiteDatabase db = getReadableDatabase();
		String query = "SELECT * FROM " + TABLE_NAME_CONVERSATION + " WHERE " + CONVERSATION_OPEN + " = 1";
		Cursor c = db.rawQuery(query, null);
		c.moveToFirst();
		String conversationUUID = c.getString(c.getColumnIndex(CONVERSATION_UUID));
		return conversationUUID;
	}
	
	public int getNextSequenceNumber(){
		
		long conversationId = getOpenConversationId();
				
		SQLiteDatabase db = getReadableDatabase();
		String query = "select max(" + MESSAGES_SEQUENCE_NUMBER + ") from "
				+ TABLE_NAME_MESSAGE + " m, " + TABLE_NAME_PARTICIPANT
				+ " p where m." + MESSAGE_SENDER_ID + " = p." + PARTICIPANT_ID
				+ " and p." + PARTICIPANT_IDENTIFICATION + "='"
				+ identification + "' and p." + PARTICIPANT_CONVERSATION_ID + " = " + conversationId + ";";
		
		Cursor c = db.rawQuery(query, null);
		c.moveToFirst();
		int nextSequenceNumber = c.getInt(0) + 1;
		Log.d(TAG, "New Sequence number: " + nextSequenceNumber);
		c.close();
		return nextSequenceNumber;
	}
	
	public String getCipherKey(long conversationId){
		SQLiteDatabase db = getReadableDatabase();
		String query = "select " + CONVERSATION_KEY + " from "
				+ TABLE_NAME_CONVERSATION + " where "
				+ CONVERSATION_ID + " = " + conversationId;
		
		Cursor c = db.rawQuery(query, null);
		c.moveToFirst();
		String key = c.getString(0);
		c.close();
		return key;
		
	}
	
	public String getCipherKey(){
		return getCipherKey(getOpenConversationId());
	}
	
	public int getCurrentParticipantSequenceNumber(String identification){
		
		long conversationId = getOpenConversationId();
		int sequenceNumber = 0;
		
		
		SQLiteDatabase db = getReadableDatabase();
		String query = "SELECT max(sequence_number) from message m, participant p WHERE m.sender_id = p._id and p.conversation_id = "
				+ conversationId
				+ " and p.identification = '"
				+ identification
				+ "';";
		
		Cursor c = db.rawQuery(query, null);
		if (c.getCount() == 0){
			sequenceNumber = 0;
		}
		else {
			c.moveToFirst();
			sequenceNumber = c.getInt(0);
			c.close();
		}
		return sequenceNumber;
	}
	
	public boolean isParticipantKnown(String identification) {
		
		boolean participantKnown = false;
		
		SQLiteDatabase db = getReadableDatabase();
		String query = "SELECT * FROM participant p WHERE p.identification = '" + identification + "';";
		Cursor c = db.rawQuery(query, null);
		if (c.getCount() > 0){
			participantKnown = true;
		}
		c.close();
		return participantKnown;
	}
}
