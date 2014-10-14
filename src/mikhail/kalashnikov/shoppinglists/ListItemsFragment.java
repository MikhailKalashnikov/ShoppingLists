package mikhail.kalashnikov.shoppinglists;

import java.util.Comparator;
import java.util.List;

import net.simonvt.messagebar.MessageBar;
import net.simonvt.messagebar.MessageBar.OnMessageClickListener;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ListItemsFragment extends SherlockListFragment 
		implements AddItemToListDialog.AddItemToListDialogListener, AddNewItemDialog.AddNewItemDialogListener, 
			AddNewItemDialog.EditListItemDialogListener,
			OnItemLongClickListener {
	private ArrayAdapter<ListItem> mAdapter;
	private ModelFragment dataModel;
	private ActionMode activeMode = null;
	private ListView listView=null;
	private MessageBar mMessageBar;
	private String mStrDeleted;
	private String mStrUndo;
	
    public static ListItemsFragment newInstance(long listId) {
    	if(LogGuard.isDebug) Log.d("ListItemsFragment", "newInstance listId" + listId);
    	ListItemsFragment f = new ListItemsFragment();

        Bundle args = new Bundle();
        args.putLong("listId", listId);
        f.setArguments(args);

        return f;
    }
    
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		
        mStrDeleted = getActivity().getString(R.string.msg_list_item_deleted);
        mStrUndo = getActivity().getString(R.string.msg_btn_undo);
        
		dataModel = ModelFragment.getInstance();
		
		mAdapter = new ShoppingListAdapter(dataModel.getListItems(getShownListId()));
		
		setListAdapter(mAdapter);
		listView = getListView();
		listView.setLongClickable(true);
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		listView.setOnItemLongClickListener(this);
		
        mMessageBar = new MessageBar(getActivity());
        mMessageBar.setOnClickListener(new OnMessageClickListener() {
			@Override
			public void onMessageClick(Parcelable token) {
				((ShoppingListAdapter) mAdapter).undoRemove();
				mMessageBar.clear();
			}
		});
        
		SwipeDismissListViewTouchListener touchListener =
            new SwipeDismissListViewTouchListener(
                    listView,
                    new SwipeDismissListViewTouchListener.OnDismissCallback() {
                        @Override
                        public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                            for (int position : reverseSortedPositions) {
                                mAdapter.remove(mAdapter.getItem(position));
                            }
                            mAdapter.notifyDataSetChanged();
                            mMessageBar.show(mStrDeleted, mStrUndo, R.drawable.ic_messagebar_undo);
                            
                        }
                    });
		listView.setOnTouchListener(touchListener);
		// Setting this scroll listener is required to ensure that during ListView scrolling,
		// we don't look for swipes.
		listView.setOnScrollListener(touchListener.makeScrollListener());
		
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()){
    		case R.id.mi_add_item_to_list:
    			SherlockDialogFragment addItemToListDialog = new AddItemToListDialog();
    			addItemToListDialog.setTargetFragment(this, 0);
    			addItemToListDialog.show(getActivity().getSupportFragmentManager(), "AddListItemDialog");
    			return true;
    			
    		case R.id.mi_clear_list:
    			SherlockDialogFragment dialog = new SherlockDialogFragment(){
    				@Override
    				public Dialog onCreateDialog(Bundle savedInstanceState) {
    					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    					builder.setMessage(R.string.clear_list_confirmation)
    						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dataModel.deleteAllListItemFromListAsync(getShownListId(), false);
									mAdapter.notifyDataSetChanged();
									
								}
							})
    						.setNegativeButton(android.R.string.cancel, null);
    					return builder.create();
    				}
    			};
    			dialog.show(getActivity().getSupportFragmentManager(), "ClearList");
    			return true;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	mAdapter.notifyDataSetChanged();
    }

    public long getShownListId() {
        return getArguments().getLong("listId", -1);
    }
	
	private ListItem getModel(int position) {
		return(((ShoppingListAdapter)getListAdapter()).getItem(position));
	}
	  
	class ShoppingListAdapter extends ArrayAdapter<ListItem>{
		ListItem removedListItem= null;
		
		ShoppingListAdapter(List<ListItem> listItems) {
			super(getActivity(), R.layout.list_item_row, R.id.item_name, listItems);
		}
		
		@Override
		public void remove(ListItem listItem) {
			removedListItem = listItem;
			dataModel.deleteListItemAsync(listItem.getId(), getShownListId());
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = super.getView(position, convertView, parent);
			ViewHolder holder = (ViewHolder) row.getTag();
			
			if(holder==null){
				holder = new ViewHolder(row);
				row.setTag(holder);
			}
			
			ListItem listItem = getModel(position);
			
			holder.qty.setText(listItem.getQty() + " - " + listItem.getItem().getQty_type());//TODO change format?
			holder.name.setText(listItem.getItem().getName()); 
			if(listItem.getIsDone()==1){
				holder.qty.setPaintFlags(holder.qty.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
				holder.qty.setBackgroundResource(R.color.doneTextViewColor);
				holder.qty.setTypeface(null, Typeface.ITALIC);
				holder.name.setPaintFlags(holder.name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
				holder.name.setBackgroundResource(R.color.doneTextViewColor);
				holder.name.setTypeface(null, Typeface.ITALIC);
			}else{
				holder.qty.setPaintFlags(holder.qty.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
				holder.qty.setTypeface(null, Typeface.NORMAL);
				holder.qty.setBackgroundResource(android.R.color.transparent);
				holder.name.setPaintFlags(holder.name.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
				holder.name.setTypeface(null, Typeface.BOLD);
				holder.name.setBackgroundResource(android.R.color.transparent);
			}
			return (row);
		}
		
		@Override
		public void notifyDataSetChanged() {
			setNotifyOnChange(false);
			sort(new Comparator<ListItem>() {
	
				@Override
				public int compare(ListItem lhs, ListItem rhs) {
					if(lhs.getIsDone()==1 && rhs.getIsDone()==0){
						return 1;
					}else if(lhs.getIsDone()==0 && rhs.getIsDone()==1){
						return -1;
					}
					return 0;
				}
			});
			super.notifyDataSetChanged();
		}

		public void undoRemove() {
			if (removedListItem!=null){
				dataModel.insertListItemAsync(removedListItem.getList_id(), removedListItem.getItem().getId(), removedListItem.getQty()); 
				notifyDataSetChanged();
			}
			
		}
	}
	
	class ViewHolder{
		TextView qty=null;
		TextView name=null;
		
		ViewHolder(View row){
			qty=(TextView) row.findViewById(R.id.item_qty);
			name=(TextView) row.findViewById(R.id.item_name);
		}
	}

	@Override
	public void onItemAddedToList(Item item) {
		dataModel.insertListItemAsync(getShownListId(), item.getId(), "1"); // TODO set qty
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onItemAdded(String name, String qty_type, String category) {
		dataModel.insertItemAndAddToListAsync(name, qty_type, category, getShownListId(), "1"); // TODO set qty
		mAdapter.notifyDataSetChanged();
		
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> view, View row,
			int position, long id) {
		listView.clearChoices();
		listView.setItemChecked(position, true);
		if (activeMode == null) {
			activeMode=getSherlockActivity().startActionMode(actionModeCallback);
		}
		return(true);
	}
	
	void editListItem(int position) {
		AddNewItemDialog dialog = AddNewItemDialog.newInstance(mAdapter.getItem(position));
		dialog.setTargetFragment(this, 0);
		dialog.show(getActivity().getSupportFragmentManager(), "EditListItemDialog");
		
	}

	void deleteItemFromList(int position) {
		dataModel.deleteListItemAsync(mAdapter.getItem(position).getId(), getShownListId());
		mAdapter.notifyDataSetChanged();
		
	}
	
	private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.list_item_context, menu);
			mode.setTitle(R.string.listitem_context_title);
			return true;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
				case R.id.mi_edit_item:
					editListItem(listView.getCheckedItemPosition());
					mode.finish();
					return true;
				
				case R.id.mi_del_item:
					deleteItemFromList(listView.getCheckedItemPosition());
					mode.finish();
					return true;
	
				default:
					return false;
			}

		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			activeMode = null;
			listView.clearChoices();
			listView.requestLayout();
			
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
		
	};

	@Override
	public void onListItemEdited(String qty, long listItem_id, long list_id, int isDone) {
		dataModel.updateListItemAsync(qty, listItem_id, list_id, isDone); 
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onListItemWithItemEdited(String qty, long listItem_id, long list_id, int isDone, 
			String name, String qty_type, String category, long item_id) {
		dataModel.updateListItemWithItemAsync(qty, listItem_id, list_id, isDone, name, qty_type, category, item_id);
		mAdapter.notifyDataSetChanged();
	}

}
