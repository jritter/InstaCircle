package ch.bfh.instacircle;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ConnectNetworkDialogFragment extends DialogFragment implements
		OnClickListener, TextWatcher {

	private static final String TAG = ConnectNetworkDialogFragment.class
			.getSimpleName();

	/*
	 * The activity that creates an instance of this dialog fragment must
	 * implement this interface in order to receive event callbacks. Each method
	 * passes the DialogFragment in case the host needs to query it.
	 */
	public interface NoticeDialogListener {
		public void onDialogPositiveClick(DialogFragment dialog);

		public void onDialogNegativeClick(DialogFragment dialog);
	}

	// Use this instance of the interface to deliver action events
	NoticeDialogListener mListener;

	private EditText txtPassword;
	private EditText txtNetworkKey;

	private String password;
	private String networkKey;

	private boolean showNetworkKeyField;
	
	private AlertDialog dialog;

	public ConnectNetworkDialogFragment(boolean showNetworkKeyField) {
		this.showNetworkKeyField = showNetworkKeyField;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();

		View view = inflater.inflate(R.layout.dialog_join_network, null);

		txtPassword = (EditText) view.findViewById(R.id.password);
		txtPassword.addTextChangedListener(this);
		
		txtNetworkKey = (EditText) view.findViewById(R.id.networkkey);
		txtNetworkKey.addTextChangedListener(this);

		if (!showNetworkKeyField) {
			txtNetworkKey.setVisibility(View.INVISIBLE);
		}

		// Inflate and set the layout for the dialog
		// Pass null as the parent view because its going in the dialog layout
		builder.setView(view);
		// Add action buttons
		builder.setPositiveButton(R.string.join,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						password = txtPassword.getText().toString();
						networkKey = txtNetworkKey.getText().toString();
						mListener
								.onDialogPositiveClick(ConnectNetworkDialogFragment.this);
					}
				});
		
		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						password = txtPassword.getText().toString();
						networkKey = txtNetworkKey.getText().toString();
						mListener
								.onDialogNegativeClick(ConnectNetworkDialogFragment.this);
					}
				});
		
		dialog = builder.create();

		
		dialog.setOnShowListener(new OnShowListener() {
			
			public void onShow(DialogInterface dialog) {
				((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
			}
		});
		
		return dialog;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Verify that the host activity implements the callback interface
		try {
			// Instantiate the NoticeDialogListener so we can send events to the
			// host
			mListener = (NoticeDialogListener) activity;
		} catch (ClassCastException e) {
			// The activity doesn't implement the interface, throw exception
			throw new ClassCastException(activity.toString()
					+ " must implement NoticeDialogListener");
		}
	}

	public String getPassword() {
		return password;
	}

	public String getNetworkKey() {
		return networkKey;
	}

	public void afterTextChanged(Editable s) {
		
		Button joinButton = ((AlertDialog)this.getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
		
		// handling the activation of the buttons
		if (showNetworkKeyField){
			// activate only if there is at least one character in the password field and 8 characters in the network key field
			if (txtPassword.getText().toString().length() < 1 || txtNetworkKey.getText().toString().length() < 8){
				joinButton.setEnabled(false);
			}
			else {
				joinButton.setEnabled(true);
			}
		}
		else {
			// activate only if there is at least one character in the password field
			if (txtPassword.getText().toString().length() < 1){
				joinButton.setEnabled(false);
			}
			else {
				joinButton.setEnabled(true);
			}
		}
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
		
	}

	public void onClick(View arg0) {
		
	}
}
