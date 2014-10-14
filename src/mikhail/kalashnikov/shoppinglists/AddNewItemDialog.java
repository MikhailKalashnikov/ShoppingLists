package mikhail.kalashnikov.shoppinglists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.actionbarsherlock.app.SherlockDialogFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class AddNewItemDialog extends SherlockDialogFragment implements OnEditorActionListener{
	public static final String KEY_MODE = "mode";
	public static final String KEY_ITEM_ID = "item_id";
	public static final String KEY_LIST_ITEM_ID = "list_item_id";
	public static final String KEY_LIST_ID = "list_id";
	public static final String KEY_QTY = "qty";
	public static final String KEY_QTY_TYPE = "qty_type";
	public static final String KEY_ITEM_NAME = "item_name";
	public static final String KEY_ITEM_CATEGORY = "item_category";
	public static final int MODE_ADD_NEW_ITEM = 0;
	public static final int MODE_EDIT_ITEM = 1;
	public static final int MODE_EDIT_LIST_ITEM = 2;
	
	private Spinner mQty_types_spinner;
	private TextView mItemNameTextView;
	private AutoCompleteTextView mItemCategoryTextView;
	private TextView mItemQtyTextView;
	private List<String> mQtyTypeList ;
	private int mMode = MODE_ADD_NEW_ITEM;
	private boolean mShowCategory = false;
	
	public static AddNewItemDialog newInstance(){
		AddNewItemDialog d = new AddNewItemDialog();
		Bundle args = new Bundle();
		args.putInt(KEY_MODE, MODE_ADD_NEW_ITEM);
		d.setArguments(args);
		return d;
	}
	
	public static AddNewItemDialog newInstance(Item item){
		AddNewItemDialog d = new AddNewItemDialog();
		Bundle args = new Bundle();
		args.putInt(KEY_MODE, MODE_EDIT_ITEM);
		args.putLong(KEY_ITEM_ID, item.getId());
		args.putString(KEY_ITEM_NAME, item.getName());
		args.putString(KEY_QTY_TYPE, item.getQty_type());
		args.putString(KEY_ITEM_CATEGORY, item.getCategory());
		d.setArguments(args);
		return d;
	}
	
	public static AddNewItemDialog newInstance(ListItem listItem){
		AddNewItemDialog d = new AddNewItemDialog();
		Bundle args = new Bundle();
		args.putInt(KEY_MODE, MODE_EDIT_LIST_ITEM);
		args.putLong(KEY_ITEM_ID, listItem.getItem().getId());
		args.putLong(KEY_LIST_ID, listItem.getList_id());
		args.putLong(KEY_LIST_ITEM_ID, listItem.getId());
		args.putString(KEY_ITEM_NAME, listItem.getItem().getName());
		args.putString(KEY_QTY_TYPE, listItem.getItem().getQty_type());
		args.putString(KEY_ITEM_CATEGORY, listItem.getItem().getCategory());
		args.putString(KEY_QTY, listItem.getQty());
		
		d.setArguments(args);
		return d;
	}
	
	public interface AddNewItemDialogListener{
		public void onItemAdded(String name, String qty_type, String category);
	}
	
	public interface EditItemDialogListener{
		public void onItemEdited(String name, String qty_type, String category, long item_id);
	}
	
	public interface EditListItemDialogListener{
		public void onListItemEdited(String qty, long listItem_id, long list_id, int isDone);
		public void onListItemWithItemEdited(String qty, long listItem_id, long list_id, int isDone, 
				String name, String qty_type, String category, long item_id);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mShowCategory = prefs.getBoolean(SettingsActivity.KEY_PREF_USE_CATEGORY, false);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		mMode = getArguments().getInt(KEY_MODE);
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.add_newitem_dialog, null);
		
		mItemNameTextView = (TextView) view.findViewById(R.id.new_item_name);
		mItemNameTextView.setOnEditorActionListener(this);
		mItemQtyTextView = (TextView) view.findViewById(R.id.new_item_qty);
		mQty_types_spinner = (Spinner) view.findViewById(R.id.new_item_qty_type);
		mQtyTypeList = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.qty_types)));
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), 
					android.R.layout.simple_spinner_item, mQtyTypeList);
		//adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mQty_types_spinner.setAdapter(adapter);
		
		mItemCategoryTextView = (AutoCompleteTextView) view.findViewById(R.id.item_category);
		if(mShowCategory){
			ShoppingListDBHelper dbHelper = ShoppingListDBHelper.getInstance(getActivity().getApplicationContext());
			List<String> category_list = dbHelper.getCategory();
			ArrayAdapter<String> category_adapter = new ArrayAdapter<String>(getActivity(), 
					android.R.layout.simple_list_item_1, category_list);
			mItemCategoryTextView.setAdapter(category_adapter);
			mItemCategoryTextView.setOnEditorActionListener(this);
		}
		
		switch (mMode) {
			case MODE_ADD_NEW_ITEM:
				mItemQtyTextView.setVisibility(View.GONE);
				break;
			case MODE_EDIT_ITEM:
				mItemQtyTextView.setVisibility(View.GONE);
				mItemNameTextView.setText(getArguments().getString(KEY_ITEM_NAME));
				mQty_types_spinner.setSelection(mQtyTypeList.indexOf(getArguments().getString(KEY_QTY_TYPE)));
				mItemCategoryTextView.setText(getArguments().getString(KEY_ITEM_CATEGORY));
				break;
			case MODE_EDIT_LIST_ITEM:
				mItemQtyTextView.setVisibility(View.VISIBLE);
				mItemQtyTextView.setText(getArguments().getString(KEY_QTY));
				mItemNameTextView.setText(getArguments().getString(KEY_ITEM_NAME));
				mQty_types_spinner.setSelection(mQtyTypeList.indexOf(getArguments().getString(KEY_QTY_TYPE)));
				mItemCategoryTextView.setText(getArguments().getString(KEY_ITEM_CATEGORY));
				break;
		}
		mItemCategoryTextView.setVisibility(mShowCategory? View.VISIBLE: View.GONE);
		
		builder
			.setTitle(mMode==MODE_EDIT_ITEM?R.string.edit_item_title:
					mMode==MODE_EDIT_LIST_ITEM?R.string.edit_list_item_title:
					R.string.add_new_item_title)
			.setView(view)
			.setPositiveButton(android.R.string.ok, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (mMode) {
						case MODE_ADD_NEW_ITEM:
							if (mItemNameTextView.getText().toString().trim().length() > 0){
								try {
									AddNewItemDialogListener listener = (AddNewItemDialogListener) getTargetFragment();
									listener.onItemAdded(mItemNameTextView.getText().toString(), mQty_types_spinner.getSelectedItem().toString(), 
											mItemCategoryTextView.getText().toString());
						        } catch (ClassCastException e) {
						            throw new ClassCastException(getTargetFragment().toString()
						                    + " must implement AddNewItemDialogListener");
						        }
							}
							break;
						case MODE_EDIT_ITEM:
							if(isItemUpdated()){
								try {
									EditItemDialogListener listener = (EditItemDialogListener) getTargetFragment();
									listener.onItemEdited(mItemNameTextView.getText().toString(), mQty_types_spinner.getSelectedItem().toString(),
											mItemCategoryTextView.getText().toString(), getArguments().getLong(KEY_ITEM_ID));
						        } catch (ClassCastException e) {
						            throw new ClassCastException(getTargetFragment().toString()
						                    + " must implement EditItemDialogListener");
						        }
							}
							break;
						case MODE_EDIT_LIST_ITEM:
							if(isListItemUpdated() || isItemUpdated()){
								try {
									EditListItemDialogListener listener = (EditListItemDialogListener) getTargetFragment();
									//listener.onListItemEdited(qty.getText().toString(), getArguments().getLong(KEY_LIST_ITEM_ID), 
									//		getArguments().getLong(KEY_LIST_ID), done.isChecked()?1:0);
									listener.onListItemWithItemEdited(mItemQtyTextView.getText().toString(), getArguments().getLong(KEY_LIST_ITEM_ID), 
													getArguments().getLong(KEY_LIST_ID), 0, 
													mItemNameTextView.getText().toString(), mQty_types_spinner.getSelectedItem().toString(),
													mItemCategoryTextView.getText().toString(), getArguments().getLong(KEY_ITEM_ID));
						        } catch (ClassCastException e) {
						            throw new ClassCastException(getTargetFragment().toString()
						                    + " must implement EditListItemDialogListener");
						        }
								
							}
							break;
					}
					
					
				}
			})
			.setNegativeButton(android.R.string.cancel, null);
		
		return builder.create();
	}

	
	private boolean isItemUpdated(){
		return !getArguments().getString(KEY_ITEM_NAME).equals(mItemNameTextView.getText().toString())
			|| !getArguments().getString(KEY_QTY_TYPE).equals(mQty_types_spinner.getSelectedItem().toString())
			|| !getArguments().getString(KEY_ITEM_CATEGORY).equals(mItemCategoryTextView.getText().toString());
	}
	
	private boolean isListItemUpdated(){
		return !getArguments().getString(KEY_QTY).equals(mItemQtyTextView.getText().toString());
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if((v.getId() == R.id.new_item_name || v.getId() == R.id.item_category) 
				&& (event == null || event.getAction() == KeyEvent.ACTION_UP)){
			InputMethodManager imm= (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0); 
			if(v.getId() == R.id.new_item_name){
				mItemCategoryTextView.requestFocus();
			}
		}
		return true;
	}
}
