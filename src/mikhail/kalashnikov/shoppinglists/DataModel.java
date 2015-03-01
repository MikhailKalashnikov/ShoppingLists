package mikhail.kalashnikov.shoppinglists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import mikhail.kalashnikov.shoppinglists.ShoppingListDBHelper.ShoppingDataListener;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

public class DataModel implements ShoppingDataListener {
	private final String TAG = getClass().getSimpleName();
	private List<ShoppingList> shoppingLists = new ArrayList<ShoppingList>();
	private Map<Long, Item> itemsMap = new HashMap<Long, Item>();
	private Map<Long, List<ListItem>> listItemsMap = new HashMap<Long, List<ListItem>>();
	//private Map<Long, List<ListItem>> listItemsMapWithCategory = new HashMap<Long, List<ListItem>>();
	private Map<Long, Map<String, List<ListItem>>> mapItemsMapWithCategory = new HashMap<Long, Map<String, List<ListItem>>>();
	private boolean mIsDataUploaded = false;
	private ShoppingListDBHelper  dbHelper;
	private static DataModel singleton =null;
	private ShoppingDataListener mListener;
	private boolean mShowCategoryOnMainScreen = true;
	
	public synchronized static final DataModel getInstance(Context context){
		Log.d("DataModel", "getInstance");
		if(singleton==null){
			singleton = new DataModel(context);
		}
		return singleton;
		
	}
	
	private DataModel(Context context){
		dbHelper = ShoppingListDBHelper.getInstance(context);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		mShowCategoryOnMainScreen = prefs.getBoolean(SettingsActivity.KEY_PREF_USE_CATEGORY_ON_MAIN_SCREEN, true);
	}
	
	public void uploadData(ShoppingDataListener listener){
		if(LogGuard.isDebug) Log.d(TAG, "uploadData = " + mIsDataUploaded);
		mListener = listener;
		if (!mIsDataUploaded){
			dbHelper.getShoppingDataAsync(this);
		}
	}
	
	@Override
	public void updateShoppingData(List<ShoppingList> shoppingLists,
			List<ListItem> listItems, Map<Long, Item> itemsMap) {
		if(LogGuard.isDebug) Log.d(TAG, "updateShoppingData");
		this.shoppingLists=shoppingLists;
		
		for(ShoppingList sl:shoppingLists){
			if(mShowCategoryOnMainScreen){
				listItemsMap.put(sl.getId(), new ArrayList<ListItem>());
				mapItemsMapWithCategory.put(sl.getId(), new HashMap<String, List<ListItem>>());
				
			}else{
				listItemsMap.put(sl.getId(), new ArrayList<ListItem>());
			}
		}
		
		this.itemsMap=itemsMap;
		
		for(ListItem li: listItems){
			if(mShowCategoryOnMainScreen){
				if(!mapItemsMapWithCategory.get(li.getList_id()).containsKey(li.getItem().getCategory())){
					mapItemsMapWithCategory.get(li.getList_id()).put(li.getItem().getCategory(), new ArrayList<ListItem>());
				}
				mapItemsMapWithCategory.get(li.getList_id()).get(li.getItem().getCategory()).add(li);
			}else{
				listItemsMap.get(li.getList_id()).add(li);
			}
		}
		if(mShowCategoryOnMainScreen){
			for(Long listId: mapItemsMapWithCategory.keySet()){
				rebuildListWithCategory(listId);
			}
		}
		mIsDataUploaded = true;
		mListener.updateShoppingData(shoppingLists, listItems, itemsMap);
	}
	
	private void rebuildListWithCategory(long listId){
		int i=0;
		listItemsMap.get(listId).clear();
		for(String category: mapItemsMapWithCategory.get(listId).keySet()){
			i++;
			listItemsMap.get(listId).add(new ListItem(-i, listId, null, category, 0));
			for(ListItem li: mapItemsMapWithCategory.get(listId).get(category)){
				listItemsMap.get(listId).add(li);
			}
		}
	}
	
	List<ListItem> getListItems(long listId){
		if(mShowCategoryOnMainScreen){
			return listItemsMap.get(listId);
		}else{
			return listItemsMap.get(listId);
		}
	}
	
	public void switchShowCategoryOnMainScreen(Boolean showCategoryOnMainScreen) {
		if(LogGuard.isDebug) Log.d(TAG, "switchShowCategoryOnMainScreen " + showCategoryOnMainScreen);
		if(mShowCategoryOnMainScreen && !showCategoryOnMainScreen){
			for(Long listId: listItemsMap.keySet()){
				Iterator<ListItem> iter = listItemsMap.get(listId).iterator();
				while(iter.hasNext()){
					ListItem li = iter.next();
					if(li.getId() < 0){
						iter.remove();
					}			
				}
			}
			
		}else if(!mShowCategoryOnMainScreen && showCategoryOnMainScreen){
			listItemsMap.clear();
			mapItemsMapWithCategory.clear();
			for(Long listId: listItemsMap.keySet()){
				mapItemsMapWithCategory.put(listId, new HashMap<String, List<ListItem>>());
				listItemsMap.put(listId, new ArrayList<ListItem>());
				for(ListItem li: listItemsMap.get(listId)){
					if(!mapItemsMapWithCategory.get(li.getList_id()).containsKey(li.getItem().getCategory())){
						mapItemsMapWithCategory.get(li.getList_id()).put(li.getItem().getCategory(), new ArrayList<ListItem>());
					}
					mapItemsMapWithCategory.get(li.getList_id()).get(li.getItem().getCategory()).add(li);
				}
				rebuildListWithCategory(listId);
			}
			
		}
		mShowCategoryOnMainScreen = showCategoryOnMainScreen;
		
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
	
	public boolean isDataUploaded(){
		return mIsDataUploaded;
	}

	void insertListItemAsync(long list_id, long item_id, String qty) {
		if(LogGuard.isDebug) Log.d(TAG, "insertListItemAsync: list_id="+list_id + ", item_id="+item_id+", qty="+qty);
		ListItem listItem = new ListItem(list_id, itemsMap.get(item_id), qty);
		
		if(mShowCategoryOnMainScreen){
			if(!mapItemsMapWithCategory.get(list_id).containsKey(listItem.getItem().getCategory())){
				mapItemsMapWithCategory.get(list_id).put(listItem.getItem().getCategory(), new ArrayList<ListItem>());
			}
			mapItemsMapWithCategory.get(list_id).get(listItem.getItem().getCategory()).add(listItem);
			rebuildListWithCategory(list_id);
		}else{
			listItemsMap.get(list_id).add(listItem);
		}
		ModelFragment.executeAsyncTask(new InsertListItemTask(listItem));
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
		if(mShowCategoryOnMainScreen){
			if(!mapItemsMapWithCategory.get(list_id).containsKey(listItem.getItem().getCategory())){
				mapItemsMapWithCategory.get(list_id).put(listItem.getItem().getCategory(), new ArrayList<ListItem>());
			}
			mapItemsMapWithCategory.get(list_id).get(listItem.getItem().getCategory()).add(listItem);
			rebuildListWithCategory(list_id);
		}else{
			listItemsMap.get(list_id).add(listItem);
		}
		ModelFragment.executeAsyncTask(new InsertItemAndAddToListTask(item, listItem));
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
			
			ModelFragment.executeAsyncTask(new InsertListItemTask(listItem));
			
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
		
		if(mShowCategoryOnMainScreen){
			mapItemsMapWithCategory.put(list.getId(), new HashMap<String, List<ListItem>>());
			listItemsMap.put(list.getId(), new ArrayList<ListItem>());
		}else{
			listItemsMap.put(list.getId(), new ArrayList<ListItem>());
		}
		
		ModelFragment.executeAsyncTask(new InsertShoppingListTask(list));
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
		if(mShowCategoryOnMainScreen){
			//release Items
			for(List<ListItem> liLists: mapItemsMapWithCategory.get(id).values()){
				for(ListItem li:liLists){
					li.getItem().releaseItem();
				}
			}
			mapItemsMapWithCategory.remove(id);
			listItemsMap.remove(id);
		}else{
			//release Items
			for(ListItem li:listItemsMap.get(id)){
				li.getItem().releaseItem();
			}
			listItemsMap.remove(id);
		}
		
		for(int i=0;i<shoppingLists.size();i++){
			if(shoppingLists.get(i).getId()==id){
				shoppingLists.remove(i);
				break;
			}
		}
		ModelFragment.executeAsyncTask(new DeleteShoppingListWithItemsTask(id));
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
				ModelFragment.executeAsyncTask(new UpdateShoppingListTask(shoppingLists.get(i)));
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
		if(LogGuard.isDebug) Log.d(TAG, "deleteListItemAsync id=" + id +",  list_id=" + list_id);
		if(mShowCategoryOnMainScreen){
			boolean isUpdated=false;
			String deletedFromCategory = null;
			for(List<ListItem> liLists: mapItemsMapWithCategory.get(list_id).values()){
				for(int i=0;i<liLists.size();i++){
					if(liLists.get(i).getId()==id){
						liLists.get(i).getItem().releaseItem();
						deletedFromCategory = liLists.get(i).getItem().getCategory();
						liLists.remove(i);
						isUpdated=true;
						break;
					}
				}
				if(isUpdated){
					break;
				}
			}
			if(deletedFromCategory!=null && mapItemsMapWithCategory.get(list_id).get(deletedFromCategory).size()==0){
				mapItemsMapWithCategory.get(list_id).remove(deletedFromCategory);
			}
			rebuildListWithCategory(list_id);
		}else{
			for(int i=0;i<listItemsMap.get(list_id).size();i++){
				if(listItemsMap.get(list_id).get(i).getId()==id){
					listItemsMap.get(list_id).get(i).getItem().releaseItem();
					listItemsMap.get(list_id).remove(i);
					break;
				}
			}
		}
		
		ModelFragment.executeAsyncTask(new DeleteListItemTask(id));
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
		
		if(mShowCategoryOnMainScreen){
			for(List<ListItem> liLists: mapItemsMapWithCategory.get(list_id).values()){
				Iterator<ListItem> iter = liLists.iterator();
				while(iter.hasNext()){
					ListItem li = iter.next();
					if(!onlyDone || li.getIsDone() == ListItem.DONE){
						li.getItem().releaseItem();
						iter.remove();
					}			
				}
			}

			if(onlyDone){
				rebuildListWithCategory(list_id);
			}else{
				mapItemsMapWithCategory.get(list_id).clear();
				listItemsMap.get(list_id).clear();
			}
		}else{
			Iterator<ListItem> iter = listItemsMap.get(list_id).iterator();
			while(iter.hasNext()){
				ListItem li = iter.next();
				if(!onlyDone || li.getIsDone() == ListItem.DONE){
					li.getItem().releaseItem();
					iter.remove();
				}			
			}
		}
		
		ModelFragment.executeAsyncTask(new DeleteAllListItemFromListTask(list_id, onlyDone));
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
		
		
		
		if(mShowCategoryOnMainScreen){
			boolean isUpdated=false;
			for(List<ListItem> liLists: mapItemsMapWithCategory.get(list_id).values()){
				for(int i=0;i<liLists.size();i++){
					if(liLists.get(i).getId()==listItem_id){
						liLists.get(i).setQty(qty);
						liLists.get(i).setIsDone(isDone);
						ModelFragment.executeAsyncTask(new UpdateListItemTask(liLists.get(i)));
						isUpdated = true;
						break;
					}
				}
				if(isUpdated){
					break;
				}
			}
			
		}else{
			for(int i=0;i<listItemsMap.get(list_id).size();i++){
				if(listItemsMap.get(list_id).get(i).getId()==listItem_id){
					listItemsMap.get(list_id).get(i).setQty(qty);
					listItemsMap.get(list_id).get(i).setIsDone(isDone);
					ModelFragment.executeAsyncTask(new UpdateListItemTask(listItemsMap.get(list_id).get(i)));
					break;
				}
			}
		}
		
	}
	
	void updateListItemWithItemAsync(String qty, long listItem_id, long list_id, int isDone,
			String name, String qty_type, String category, long item_id) {
		if(LogGuard.isDebug) Log.d(TAG, "updateListItemWithItemAsync: listItem_id="+listItem_id + ", list_id="+list_id
				+ ", qty="+qty + ", isDone="+isDone
				+ ", item_id="+item_id + ", name="+name + ", qty_type="+qty_type + ", category="+category);
		
		String oldCategory = itemsMap.get(item_id).getCategory();
		if(mShowCategoryOnMainScreen && ((oldCategory ==null && category!=null)
				|| (oldCategory !=null && category==null)
				|| !oldCategory.equals(category))){
			
			ListItem listItem=null;
			Iterator<ListItem> iter = mapItemsMapWithCategory.get(list_id).get(oldCategory).iterator();
			while(iter.hasNext()){
				ListItem li = iter.next();
				if(li.getId() == listItem_id){
					listItem = li;
					iter.remove();
					break;
				}
			}
			
			if(mapItemsMapWithCategory.get(list_id).get(oldCategory).size() == 0){
				mapItemsMapWithCategory.get(list_id).remove(oldCategory);
			}
			
			if(!mapItemsMapWithCategory.get(list_id).containsKey(category)){
				mapItemsMapWithCategory.get(list_id).put(category, new ArrayList<ListItem>());
			}
			mapItemsMapWithCategory.get(list_id).get(category).add(listItem);
			rebuildListWithCategory(list_id);
		}
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
