package mikhail.kalashnikov.shoppinglists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.actionbarsherlock.app.SherlockDialogFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup; 
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

public class AddItemToListDialog extends SherlockDialogFragment{
	private static final String ATTR_GROUP_NAME = "groupName";
	private static final String ATTR_ITEM_NAME = "itemName";
	private List<List<Map<String, Item>>> childData;
	private List<Item> items;
	private int[] childTo;
	private String[] childFrom;
	
	public interface AddItemToListDialogListener{
		public void onItemAddedToList(Item item);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		setStyle(STYLE_NORMAL, R.style.AppBaseTheme);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		boolean showCategory = prefs.getBoolean(SettingsActivity.KEY_PREF_USE_CATEGORY, false);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.add_item_to_list, null);
		
//		Button addNewItem = (Button) view.findViewById(R.id.add_new_item);
//		addNewItem.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				showAddNewItemDialog();
//			}
//		});
		
		ExpandableListView expListView = (ExpandableListView) view.findViewById(R.id.item_list_with_category);
		ListView listView = (ListView) view.findViewById(R.id.item_list);
		View headerView = inflater.inflate(R.layout.header_add_new_item, null);
		headerView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showAddNewItemDialog();
			}
		});
		
		if(showCategory){
			expListView.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
			
			String empty_category = getActivity().getResources().getString(R.string.empty_category);
			Map<String, List<Item>> getCategoryItemMap = ModelFragment.getInstance().getCategoryItemMap(); 
			List<Map<String, String>> groupData = new ArrayList<Map<String,String>>();
			for(String category: getCategoryItemMap.keySet()){
				Map<String, String> m = new HashMap<String, String>();
				m.put(ATTR_GROUP_NAME, category == null? empty_category: category);
				groupData.add(m);
			}
			
			String[] groupFrom = new String[]{ATTR_GROUP_NAME};
			int[] groupTo = new int[]{android.R.id.text1};
			
			childData = new ArrayList<List<Map<String,Item>>>();
			for(String category: getCategoryItemMap.keySet()){
				childData.add(buildChildDataItem(getCategoryItemMap.get(category)));
			}
			
			childFrom  = new String[]{ATTR_ITEM_NAME};
			childTo = new int[]{android.R.id.text1};
			
	        SimpleExpandableListAdapter adapter = new SimpleExpandableListAdapter(
	        		getActivity(), 
	        		groupData, 
	        		android.R.layout.simple_expandable_list_item_1, 
	        		groupFrom, 
	        		groupTo, 
	        		childData, 
	        		android.R.layout.simple_list_item_1, 
	        		childFrom, 
	        		childTo){
			            @Override
			            public View getChildView(int groupPosition, int childPosition,
			                    boolean isLastChild, View convertView, ViewGroup parent) {
			                
			                View v;
			                if (convertView == null) {
			                    v = newChildView(isLastChild, parent);
			                } else {
			                    v = convertView;
			                }
			                bindView(v, childData.get(groupPosition).get(childPosition), childFrom, childTo);
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
	        
	        expListView.addHeaderView(headerView);
	        expListView.setAdapter(adapter);
			expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
				
				@Override
				public boolean onChildClick(ExpandableListView parent, View v,
						int groupPosition, int childPosition, long id) {
					addItemToList(childData.get(groupPosition).get(childPosition).get(ATTR_ITEM_NAME));
					return false;
				}
			});
			
			builder.setTitle(R.string.mi_add_item).setView(view);
		}else{
			expListView.setVisibility(View.GONE);
			listView.setVisibility(View.VISIBLE);
			items = ModelFragment.getInstance().getItems(); 
			ArrayAdapter<Item> adapter = new ArrayAdapter<Item>(getActivity(), android.R.layout.simple_list_item_1, items);
			listView.addHeaderView(headerView);
			listView.setAdapter(adapter);
			listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> adapter, View view,
						int position, long id) {
					addItemToList(items.get(position-1));
				}
				
			});
			
			builder.setTitle(R.string.mi_add_item).setView(view);
		}
		
		return builder.create();
	}


	private void addItemToList(Item item){
		try {
			AddItemToListDialogListener listener = (AddItemToListDialogListener) getTargetFragment();
			listener.onItemAddedToList(item);
			dismiss();
        } catch (ClassCastException e) {
            throw new ClassCastException(getTargetFragment().toString()
                    + " must implement AddItemToListDialogListener");
        }
	}
	
	private void showAddNewItemDialog(){
		SherlockDialogFragment addNewItemDialog = AddNewItemDialog.newInstance();
		addNewItemDialog.setTargetFragment(getTargetFragment(), 0);
		addNewItemDialog.show(getActivity().getSupportFragmentManager(), "AddNewItemDialog");
        dismiss();
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
	
	
}
