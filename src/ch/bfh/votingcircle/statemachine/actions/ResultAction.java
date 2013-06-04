package ch.bfh.votingcircle.statemachine.actions;

import java.io.Serializable;


import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import ch.bfh.instacircle.R;

import ch.bfh.votingcircle.EvotingMainActivity;
import ch.bfh.votingcircle.ResultActivity;
import ch.bfh.votingcircle.entities.DataManager;

import com.continuent.tungsten.fsm.core.Action;
import com.continuent.tungsten.fsm.core.Entity;
import com.continuent.tungsten.fsm.core.Event;
import com.continuent.tungsten.fsm.core.Transition;
import com.continuent.tungsten.fsm.core.TransitionFailureException;
import com.continuent.tungsten.fsm.core.TransitionRollbackException;

/**
 * Action executed in Exit step
 * This action show a UI with the results
 * @author Philémon von Bergen
 * 
 */
public class ResultAction implements Action {

	private DataManager dm;
	
	public ResultAction(DataManager dm){
		this.dm = dm;
	}
	
	@Override
	public void doAction(Event message, Entity entity, Transition transition,
			int actionType)
					throws TransitionRollbackException, TransitionFailureException,
					InterruptedException {

		if(!dm.isActive()) return;
		
		//Dismiss progress dialog
		EvotingMainActivity activity = dm.getEvotingMainActivity();
		activity.dismissProgressDialog();
		//Show result UI
		if(dm.getResult().size()!=0){
			Intent intent = new Intent(dm.getEvotingMainActivity(),ResultActivity.class);
			dm.getEvotingMainActivity().startActivity(intent);
		}

		//Enable button to show result
		Button b2 = (Button)activity.findViewById(R.id.start_button);
		b2.setText(R.string.result);
		b2.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(dm.getEvotingMainActivity(),ResultActivity.class);
				intent.putExtra("result", (Serializable)dm.getResult());
				dm.getEvotingMainActivity().startActivity(intent);
			}
		});
		b2.setVisibility(View.VISIBLE);
		b2.setEnabled(true);

		View v6 = (View) activity.findViewById(R.id.evoting_main_view_border_2);
		v6.setVisibility(View.VISIBLE);

		View v4 = (View) activity.findViewById(R.id.evoting_main_view_space_3);
		v4.setVisibility(View.VISIBLE);

		View v5 = (View) activity.findViewById(R.id.evoting_main_view_space_4);
		v5.setVisibility(View.VISIBLE);

		activity.setTitle(R.string.terminated);

	}

}
