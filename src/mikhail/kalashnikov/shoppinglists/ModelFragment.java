package mikhail.kalashnikov.shoppinglists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.actionbarsherlock.app.SherlockFragment;

import mikhail.kalashnikov.shoppinglists.ShoppingListDBHelper.ShoppingDataListener;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class ModelFragment extends SherlockFragment implements ShoppingDataListener {
	private final String TAG = getClass().getSimpleName();
	private List<ShoppingList> shoppingLists = new ArrayList<ShoppingList>();
	private Map<Long, Item> itemsMap = new HashMap<Long, Item>();
	private Map<Long, List<ListItem>> listItemsMap = new HashMap<Long, List<ListItem>>();
	private boolean isDataUploaded = false;
	private ShoppingListDBHelper  dbHelper;
	private static ModelFragment singleton =null;
	
	synchronized static ModelFragment getInstance(){
		if(singleton==null){
			singleton = new ModelFragment();
		}
		return singleton;
		
	}
	
	private ModelFragment(){
		super();
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);
		dbHelper = ShoppingListDBHelper.getInstance(getActivity().getApplicationContext());
		uploadData();
	}
	
	synchronized public void uploadData(){
		if (isDataUploaded){
			((ShoppingListsActivity)getActivity()).showListItems(shoppingLists);
		}else {
			dbHelper.getShoppingDataAsync(this);
		}
	}
	
	@TargetApi(11)
	static public <T> void executeAsyncTask(AsyncTask<T, ?, ?> task, T... params) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
		}
		else {
			task.execute(params);
		}
	}
	
	@Override
	public void updateShoppingData(List<ShoppingList> shoppingLists,
			List<ListItem> listItems, Map<Long, Item> itemsMap) {
		this.shoppingLists=shoppingLists;
		
		for(ShoppingList sl:shoppingLists){
			listItemsMap.put(sl.getId(), new ArrayList<ListItem>());
		}
		
		this.itemsMap=itemsMap;
		
		for(ListItem li: listItems){
			listItemsMap.get(li.getList_id()).add(li);
		}
		isDataUploaded=true;
		uploadData();
	}
	

	List<ListItem> getListItems(long listId){
		return listItemsMap.get(listId);
	}
	
	List<ShoppingList> getShoppingList(){
		return shoppingLists;
	}

	List<Item> getItems() {
		return dbHelper.getItems();
	}
	List<String> getCategory() {
		return dbHelper.getCategory();
	}
	
	Map<String, List<Item>> getCategoryItemMap(){
		return dbHelper.getCategoryItemMap();
	}

	void insertListItemAsync(long list_id, long item_id, String qty) {
		if(LogGuard.isDebug) Log.d(TAG, "insertListItemAsync: list_id="+list_id + ", item_id="+item_id+", qty="+qty);
		ListItem listItem = new ListItem(list_id, itemsMap.get(item_id), qty);
		listItemsMap.get(list_id).add(listItem);
		executeAsyncTask(new InsertListItemTask(listItem));
	}
	
	private class InsertListItemTask extends AsyncTask<Void, Void, Void>{
		private ListItem listItem;
		private long newRowId;
		
		InsertListItemTask(ListItem listItem){
			this.listItem=listItem;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			newRowId = dbHelper.insertListItem(listItem);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			listItem.setId(newRowId);
		}
		
	}
	
	void insertItemAndAddToListAsync(String name, String qty_type, String category, long list_id, String qty) {
		Item item = new Item(name, qty_type, category);
		ListItem listItem = new ListItem(list_id, item, qty);
		listItemsMap.get(list_id).add(listItem);
		executeAsyncTask(new InsertItemAndAddToListTask(item, listItem));
	}

	private class InsertItemAndAddToListTask extends AsyncTask<Void, Void, Void>{
		private Item item;
		private ListItem listItem;
		private long newRowId;
		private boolean itemExists = false;
		
		InsertItemAndAddToListTask(Item item, ListItem listItem){
			this.item=item;
			this.listItem=listItem;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			// try to find the same item in the list
			for(Item i: itemsMap.values()){
				if (item.compareTo(i) == 0){
					item = i;
					itemExists = true;
					break;
				}
			}
			if(!itemExists){
				newRowId = dbHelper.insertItem(item);
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(!itemExists){
				item.setId(newRowId);
				itemsMap.put(newRowId, item);
				dbHelper.addItemToList(item);
			}else{
				listItem.setItem(item);
			}
			
			executeAsyncTask(new InsertListItemTask(listItem));
			
		}
		
	}
	
	void insertItemAsync(String name, String qty_type, String category) {
		dbHelper.insertItemAsync(name, qty_type, category);
	}
	
	long insertShoppingListAsync(String name) {
		long id = dbHelper.getNextShoppingListId();
		ShoppingList list = new ShoppingList(name, id);
		if(LogGuard.isDebug) Log.d(TAG, "insertShoppingListAsync: "+list);
		shoppingLists.add(list);
		listItemsMap.put(list.getId(), new ArrayList<ListItem>());
		executeAsyncTask(new InsertShoppingListTask(list));
		return id;
	}
	
	private class InsertShoppingListTask extends AsyncTask<Void, Void, Void>{
		private ShoppingList list;
		
		InsertShoppingListTask(ShoppingList list){
			this.list=list;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			dbHelper.insertShoppingList(list);
			return null;
		}
		
	}
	
	void deleteShoppingListWithItemsAsync(long id) {
		if(LogGuard.isDebug) Log.d(TAG, "deleteShoppingListWithItemsAsync: list_id="+id);
		//release Items
		for(ListItem li:listItemsMap.get(id)){
			li.getItem().releaseItem();
		}
		listItemsMap.remove(id);
		for(int i=0;i<shoppingLists.size();i++){
			if(shoppingLists.get(i).getId()==id){
				shoppingLists.remove(i);
				break;
			}
		}
		executeAsyncTask(new DeleteShoppingListWithItemsTask(id));
	}
	
	private class DeleteShoppingListWithItemsTask extends AsyncTask<Void, Void, Void>{
		private long id;
		
		DeleteShoppingListWithItemsTask(long id){
			this.id=id;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			dbHelper.deleteItemsFromShoppingList(id);
			dbHelper.deleteShoppingList(id);
			return null;
		}

	}
	
	void updateShoppingListAsync(String name, long id) {
		if(LogGuard.isDebug) Log.d(TAG, "updateShoppingListAsync: list_id="+id + ", name="+name);
		for(int i=0;i<shoppingLists.size();i++){
			if(shoppingLists.get(i).getId() == id){
				shoppingLists.get(i).setName(name);
				executeAsyncTask(new UpdateShoppingListTask(shoppingLists.get(i)));
				break;
			}
		}
		
	}
	
	private class UpdateShoppingListTask extends AsyncTask<Void, Void, Void>{
		private ShoppingList list;
		
		UpdateShoppingListTask(ShoppingList list){
			this.list=list;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			dbHelper.updateShoppingList(list);
			return null;
		}
		
	}
	
	void deleteListItemAsync(long id, long list_id) {
		for(int i=0;i<listItemsMap.get(list_id).size();i++){
			if(listItemsMap.get(list_id).get(i).getId()==id){
				listItemsMap.get(list_id).get(i).getItem().releaseItem();
				listItemsMap.get(list_id).remove(i);
				break;
			}
		}
		executeAsyncTask(new DeleteListItemTask(id));
	}
	
	private class DeleteListItemTask extends AsyncTask<Void, Void, Void>{
		private long id;
		
		DeleteListItemTask(long id){
			this.id=id;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			dbHelper.deleteListItem(id);
			return null;
		}

	}
	
	void deleteAllListItemFromListAsync(long list_id, boolean onlyDone) {
		if(LogGuard.isDebug) Log.d(TAG, "deleteAllListItemFromListAsync: list_id=" + list_id
				+ ", onlyDone=" + onlyDone);
		
		
		Iterator<ListItem> iter = listItemsMap.get(list_id).iterator();
		while(iter.hasNext()){
			ListItem li = iter.next();
			if(!onlyDone || li.getIsDone() == ListItem.DONE){
				li.getItem().releaseItem();
				iter.remove();
			}			
		}
		
		executeAsyncTask(new DeleteAllListItemFromListTask(list_id, onlyDone));
	}
	
	private class DeleteAllListItemFromListTask extends AsyncTask<Void, Void, Void>{
		private long list_id;
		private boolean onlyDone;
		
		DeleteAllListItemFromListTask(long list_id, boolean onlyDone){
			this.list_id=list_id;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			if (onlyDone){
				dbHelper.deleteDoneItemsFromShoppingList(list_id);
			}else{
				dbHelper.deleteItemsFromShoppingList(list_id);
			}
			return null;
		}

	}
	
	void updateListItemAsync(String qty, long listItem_id, long list_id, int isDone) {
		if(LogGuard.isDebug) Log.d(TAG, "updateListItemAsync: listItem_id="+listItem_id + ", list_id="+list_id
				+ ", qty="+qty + ", isDone="+isDone);
		
		for(int i=0;i<listItemsMap.get(list_id).size();i++){
			if(listItemsMap.get(list_id).get(i).getId()==listItem_id){
				listItemsMap.get(list_id).get(i).setQty(qty);
				listItemsMap.get(list_id).get(i).setIsDone(isDone);
				executeAsyncTask(new UpdateListItemTask(listItemsMap.get(list_id).get(i)));
				break;
			}
		}
		
	}
	
	void updateListItemWithItemAsync(String qty, long listItem_id, long list_id, int isDone,
			String name, String qty_type, String category, long item_id) {
		if(LogGuard.isDebug) Log.d(TAG, "updateListItemWithItemAsync: listItem_id="+listItem_id + ", list_id="+list_id
				+ ", qty="+qty + ", isDone="+isDone
				+ ", item_id="+item_id + ", name="+name + ", qty_type="+qty_type + ", category="+category);
		
		dbHelper.updateItemAsync(name, qty_type, item_id, category);	
		updateListItemAsync(qty, listItem_id, list_id, isDone);
	}
	
	private class UpdateListItemTask extends AsyncTask<Void, Void, Void>{
		private ListItem listItem;
		
		UpdateListItemTask(ListItem listItem){
			this.listItem=listItem;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			dbHelper.updateListItem(listItem);
			return null;
		}
		
	}
}
	
	
