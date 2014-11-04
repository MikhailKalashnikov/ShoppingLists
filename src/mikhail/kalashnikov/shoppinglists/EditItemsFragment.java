package mikhail.kalashnikov.shoppinglists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class EditItemsFragment extends SherlockFragment 
		implements AddNewItemDialog.EditItemDialogListener, 
			OnItemLongClickListener {
	private final String TAG = getClass().getSimpleName();
	private ArrayAdapter<Item> mAdapter;
	private SimpleExpandableListAdapter mExpandableListAdapter;
	private ShoppingListDBHelper mDBHelper;
	private ActionMode mActiveMode = null;
	private ListView mListView=null;
	private boolean mShowCategory;
	private static final String ATTR_GROUP_NAME = "groupName";
	private static final String ATTR_ITEM_NAME = "itemName";
	private List<List<Map<String, Item>>> mChildData;
	private List<Item> mItems;
	private int[] mChildTo;
	private String[] mChildFrom;
	private DataModel mModel;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mShowCategory = prefs.getBoolean(SettingsActivity.KEY_PREF_USE_CATEGORY, true);
		mModel = DataModel.getInstance(getActivity().getApplicationContext());
		mDBHelper = ShoppingListDBHelper.getInstance(getSherlockActivity());
	}
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.add_item_to_list, container, false);
		
//		mAdapter = new ItemsListAdapter(mDBHelper.getItems());
//		mListView = (ListView) view.findViewById(R.id.edit_items_list);
//		mListView.setLongClickable(true);
//		mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
//		mListView.setOnItemLongClickListener(this);
//		mListView.setOnItemClickListener(this);
//		mListView.setAdapter(mAdapter);
		
		if(mShowCategory){
			mListView = (ExpandableListView) view.findViewById(R.id.item_list_with_category);
			mListView.setVisibility(View.VISIBLE);
			view.findViewById(R.id.item_list).setVisibility(View.GONE);
			
			String empty_category = getActivity().getResources().getString(R.string.empty_category);
			Map<String, List<Item>> getCategoryItemMap = mModel.getCategoryItemMap(); 
			List<Map<String, String>> groupData = new ArrayList<Map<String,String>>();
			for(String category: getCategoryItemMap.keySet()){
				Map<String, String> m = new HashMap<String, String>();
				m.put(ATTR_GROUP_NAME, category == null? empty_category: category);
				groupData.add(m);
			}
			
			String[] groupFrom = new String[]{ATTR_GROUP_NAME};
			int[] groupTo = new int[]{android.R.id.text1};
			
			mChildData = new ArrayList<List<Map<String,Item>>>();
			for(String category: getCategoryItemMap.keySet()){
				mChildData.add(buildChildDataItem(getCategoryItemMap.get(category)));
			}
			
			mChildFrom  = new String[]{ATTR_ITEM_NAME};
			mChildTo = new int[]{android.R.id.text1};
			
	        mExpandableListAdapter = new SimpleExpandableListAdapter(
	        		getActivity(), 
	        		groupData, 
	        		android.R.layout.simple_expandable_list_item_1, 
	        		groupFrom, 
	        		groupTo, 
	        		mChildData, 
	        		android.R.layout.simple_list_item_1, 
	        		mChildFrom, 
	        		mChildTo){
			            @Override
			            public View getChildView(int groupPosition, int childPosition,
			                    boolean isLastChild, View convertView, ViewGroup parent) {
			                
			                View v;
			                if (convertView == null) {
			                    v = newChildView(isLastChild, parent);
			                } else {
			                    v = convertView;
			                }
			                bindView(v, mChildData.get(groupPosition).get(childPosition), mChildFrom, mChildTo);
			                return v;
			            }
			            
			            private void bindView(View view, Map<String, Item> data, String[] from, int[] to) {
			                int len = to.length;

			                for (int i = 0; i < len; i++) {
			                    TextView v = (TextView)view.findViewById(to[i]);
			                    if (v != null) {
			                        v.setText(data.get(from[i]).getName());
			                    }
			                }
			            }
	        };  
	        
	        ((ExpandableListView)mListView).setAdapter(mExpandableListAdapter);
	        ((ExpandableListView)mListView).setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
				
				@Override
				public boolean onChildClick(ExpandableListView parent, View v,
						int groupPosition, int childPosition, long id) {
					editItem(mChildData.get(groupPosition).get(childPosition).get(ATTR_ITEM_NAME));
					return false;
				}
			});
			
			for(int i=0;i<groupData.size();i++){
				((ExpandableListView)mListView).expandGroup(i);
			}
			
		}else{
			mListView = (ListView) view.findViewById(R.id.item_list);
			mListView.setVisibility(View.VISIBLE);
			view.findViewById(R.id.item_list_with_category).setVisibility(View.GONE);

			mItems = mModel.getItems(); 
			mAdapter = new ArrayAdapter<Item>(getActivity(), android.R.layout.simple_list_item_1, mItems);
			mListView.setAdapter(mAdapter);
			mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> adapter, View view,
						int position, long id) {
					editItem(mItems.get(position));
				}
				
			});
			
		}
		
		mListView.setLongClickable(true);
		mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		mListView.setOnItemLongClickListener(this);
		
		return view;
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> view, View row,
			int position, long id) {
		mListView.clearChoices();
		mListView.setItemChecked(position, true);
		if (mActiveMode == null) {
			mActiveMode= getSherlockActivity().startActionMode(actionModeCallback);
		}
		return(true);
	}
	
	void editItem(Item item) {
		Log.d(TAG, item.toStringFull());
		AddNewItemDialog dialog = AddNewItemDialog.newInstance(item);
		dialog.setTargetFragment(this, 0);
		dialog.show(getActivity().getSupportFragmentManager(), "EditItemDialog");
	}


	void deleteItem(Item item, ExpandableListPosition expandableListPosition) {
		if(item.isUsed()){
			SherlockDialogFragment dialog = new SherlockDialogFragment(){
				@Override
				public Dialog onCreateDialog(Bundle savedInstanceState) {
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					builder.setMessage(R.string.item_is_used)
						.setPositiveButton(android.R.string.ok, null);
					return builder.create();
				}
			};
			dialog.show(getActivity().getSupportFragmentManager(), "DeleteItemError");
		}else{
			mDBHelper.deleteItemAsync(item.getId());
			if(mShowCategory){
				mChildData.get(expandableListPosition.group).remove(expandableListPosition.child_position);
				mExpandableListAdapter.notifyDataSetChanged();
			}else{
				mAdapter.remove(item);
				mAdapter.notifyDataSetChanged();
			}
		}
		
	}

	private List<Map<String,Item>> buildChildDataItem(List<Item> groupItems){
    	List<Map<String,Item>> childDataItem = new ArrayList<Map<String,Item>>();
    	for(Item i: groupItems){
    		Map<String, Item> m = new HashMap<String, Item>();
    		m.put(ATTR_ITEM_NAME, i);
    		childDataItem.add(m);
    	}
    	return childDataItem;
    }
	
	@Override
	public void onItemEdited(String name, String qty_type, String category, long item_id) {
		mDBHelper.updateItemAsync(name, qty_type, item_id, category); 
		if(mShowCategory){
			mExpandableListAdapter.notifyDataSetChanged();
		}else{
			mAdapter.notifyDataSetChanged();
		}
	}
 
	
	private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.list_item_context, menu);
			mode.setTitle(R.string.listitem_context_title);
			if(mShowCategory){
				if(getExpandableListPosition(mListView.getCheckedItemPosition()).type == ExpandableListPosition.TYPE_GROUP){
					return false;
				}
			}
			return true;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
				case R.id.mi_edit_item:
					int position = mListView.getCheckedItemPosition();
					
					if(mShowCategory){
						ExpandableListPosition pos = getExpandableListPosition(position);
						if(pos.type == ExpandableListPosition.TYPE_CHILD){
							editItem(mChildData.get(pos.group).get(pos.child_position).get(ATTR_ITEM_NAME));
						}
					}else{
						editItem(mAdapter.getItem(position));
					}
					mode.finish();
					return true;
				
				case R.id.mi_del_item:
					position = mListView.getCheckedItemPosition();
					
					if(mShowCategory){
						ExpandableListPosition pos = getExpandableListPosition(position);
						if(pos.type == ExpandableListPosition.TYPE_CHILD){
							deleteItem(mChildData.get(pos.group).get(pos.child_position).get(ATTR_ITEM_NAME), pos);
						}
					}else{
						deleteItem(mAdapter.getItem(position), null);
					}
					mode.finish();
					return true;
	
				default:
					return false;
			}

		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActiveMode = null;
			mListView.clearChoices();
			mListView.requestLayout();
			
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
		
	};

	private ExpandableListPosition getExpandableListPosition(int position){
		ExpandableListPosition pos = new ExpandableListPosition();
		pos.type = ExpandableListPosition.TYPE_GROUP;
		int current_pos = -1;
		for(int i=0; i< mChildData.size(); i++){
			current_pos++;
			if(current_pos == position){
				pos.type = ExpandableListPosition.TYPE_GROUP;
				pos.group = i;
				return pos;
			}
			
			if(position <= current_pos + mChildData.get(i).size()){
				pos.type = ExpandableListPosition.TYPE_CHILD;
				pos.group = i;
				pos.child_position = position - current_pos - 1;
				return pos;
			}
			
			current_pos = current_pos + mChildData.get(i).size();
		}
		
		return pos;
	}
	
	private class ExpandableListPosition{
		private static final int TYPE_GROUP = 1;
		private static final int TYPE_CHILD = 2;
		int type;
		int group;
		int child_position;
		@Override
		public String toString() {
			return "ExpandableListPosition [type=" + type + ", group=" + group
					+ ", child_position=" + child_position + "]";
		}
		
	}

}
