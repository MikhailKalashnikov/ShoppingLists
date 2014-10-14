package mikhail.kalashnikov.shoppinglists;

import com.actionbarsherlock.app.SherlockDialogFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class AddNewListDialog extends SherlockDialogFragment{
	public static final int NEW_LIST_ID = -1;
	private TextView name;
	private AddNewListDialogListener listener;
	
	public interface AddNewListDialogListener{
		public void onListAdded(String name);
		public void onListEdited(String name,long id);
	}
	
	static AddNewListDialog newInstance(String name, long id){
		AddNewListDialog dialog = new AddNewListDialog();
		Bundle args = new Bundle();
		args.putString("name", name);
		args.putLong("id", id);
		dialog.setArguments(args);
		return dialog;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.add_newlist_dialog, null);
		name = (TextView) view.findViewById(R.id.new_list_name);
		boolean newListMode = true;
		if(getArguments().getLong("id") != NEW_LIST_ID){
			newListMode=false;
			name.setText(getArguments().getString("name"));
		}
		builder
			.setTitle(newListMode?R.string.add_new_list_title:R.string.edit_list_title)
			.setView(view)
			.setPositiveButton(android.R.string.ok, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (getArguments().getLong("id") == NEW_LIST_ID){ //add new
						if (name.getText().toString().trim().length() > 0){
							listener.onListAdded(name.getText().toString());
						}
					}else if(!getArguments().getString("name").equals(name.getText().toString()) 
							&& name.getText().toString().trim().length() > 0){
						listener.onListEdited(name.getText().toString(), getArguments().getLong("id"));
					}
					
				}
			})
			.setNegativeButton(android.R.string.cancel, null);
		
		return builder.create();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			listener = (AddNewListDialogListener) activity;
		} catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement AddNewListDialogListener");
        }
	}

}
