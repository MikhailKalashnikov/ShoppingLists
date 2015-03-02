package mikhail.kalashnikov.shoppinglists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

public class ShoppingListDBHelper extends SQLiteOpenHelper {
	private final String TAG = getClass().getSimpleName();
	private static final String DATABASE_NAME="ShoppingList.db"; 
	private static final int SCHEMA_VERSION=1;
	private static ShoppingListDBHelper singleton=null;
	private List<ShoppingList> shoppingLists; 
	private List<ListItem> listItems; 
	private List<Item> mItems; 
	private Map<String, List<Item>> categoriesMap = null; 
	private Map<Long, Item> itemsMap = new HashMap<Long, Item>();
	private Context ctxt=null;
	private long maxShoppingListId=-1;
	private String emptyCategoryName;
	
	synchronized static ShoppingListDBHelper getInstance(Context ctxt){
		if(singleton==null){
			singleton = new ShoppingListDBHelper(ctxt.getApplicationContext());
		}
		return singleton;
		
	}
	
	private ShoppingListDBHelper(Context ctxt){
		super(ctxt, DATABASE_NAME, null, SCHEMA_VERSION);
		this.ctxt = ctxt;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		try{
			db.beginTransaction();
			db.execSQL("CREATE TABLE " + ShoppingList.TABLE_NAME + "("
					+ ShoppingList._ID + " INTEGER,"
					+ ShoppingList.COLUMN_NAME + " TEXT"
					+ ");");
			
			ContentValues cv = new ContentValues();
			cv.put(ShoppingList.COLUMN_NAME, ctxt.getResources().getString(R.string.shopping_list_default_name));
			cv.put(ShoppingList._ID, 1);
			db.insert(ShoppingList.TABLE_NAME, null, cv);
			
			db.execSQL("CREATE TABLE " + ListItem.TABLE_NAME + "("
					+ ListItem._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ ListItem.COLUMN_LIST_ID + " INTEGER,"
					+ ListItem.COLUMN_ITEM_ID + " INTEGER,"
					+ ListItem.COLUMN_QTY + " TEXT," 
					+ ListItem.COLUMN_DONE + " INTEGER"
					+ ");");
			
			db.execSQL("CREATE TABLE " + Item.TABLE_NAME + "("
					+ Item._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ Item.COLUMN_NAME + " TEXT,"
					+ Item.COLUMN_QTY_TYPE + " TEXT,"
					+ Item.COLUMN_CATEGORY + " TEXT"
					+ ");");
			
			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		throw new RuntimeException(ctxt.getString(R.string.on_upgrade_error));
	}
	
	void getShoppingDataAsync(ShoppingDataListener listener) { 
		ModelFragment.executeAsyncTask(new GetShoppingDataTask(listener));
	}
	
	long getNextShoppingListId(){
		maxShoppingListId++;
		return maxShoppingListId;
	}
	interface ShoppingDataListener {
		void updateShoppingData(List<ShoppingList> shoppingLists, 
								List<ListItem> listItems, 
								Map<Long, Item> itemsMap);
	}
	
	private class GetShoppingDataTask extends AsyncTask<Void, Void, Void>{
		private ShoppingDataListener listener;
		
		GetShoppingDataTask(ShoppingDataListener listener){
			this.listener = listener;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			try{
				Cursor c = getReadableDatabase().rawQuery(
							"SELECT " +
								ShoppingList._ID + "," +
								ShoppingList.COLUMN_NAME + 
							" FROM " + ShoppingList.TABLE_NAME, 
							null);
				
				shoppingLists = new ArrayList<ShoppingList>();
				while (c.moveToNext()) { 
					long id=c.getLong(0);
					if(id>maxShoppingListId){
						maxShoppingListId=id;
					}
					String name=c.getString(1); 
					shoppingLists.add(new ShoppingList(name, id));
				}
				
				c.close();
				if(LogGuard.isDebug) {
					for(ShoppingList s:shoppingLists){
						Log.d(TAG, s.toString());
					}
				}
				
				c = getReadableDatabase().rawQuery(
						"SELECT " +
							Item._ID + "," +
							Item.COLUMN_NAME + "," +
							Item.COLUMN_QTY_TYPE + "," +
							Item.COLUMN_CATEGORY +
						" FROM " + Item.TABLE_NAME, 
						null);
			
				mItems = new ArrayList<Item>();
				while (c.moveToNext()) { 
					long id=c.getLong(0);
					String name=c.getString(1);
					String qty_type=c.getString(2);
					String category=c.getString(3);
					Item item = new Item(id, name, qty_type,category);
					itemsMap.put(id, item);
					mItems.add(item);
				}
				Collections.sort(mItems);
				c.close();
				if(LogGuard.isDebug) {
					
					for(Item s:itemsMap.values()){
						Log.d(TAG, s.toStringFull());
					}
				}
				
				c = getReadableDatabase().rawQuery(
						"SELECT " +
							ListItem._ID + "," +
							ListItem.COLUMN_LIST_ID + "," +
							ListItem.COLUMN_ITEM_ID + "," +
							ListItem.COLUMN_QTY + "," +
							ListItem.COLUMN_DONE +
						" FROM " + ListItem.TABLE_NAME, 
						null);
			
				listItems = new ArrayList<ListItem>();
				while (c.moveToNext()) { 
					long id=c.getLong(0);
					long list_id=c.getLong(1);
					long item_id=c.getLong(2);
					String qty=c.getString(3);
					int isDone=c.getInt(4);
					listItems.add(new ListItem(id, list_id, itemsMap.get(item_id), qty, isDone));
				}
				
				c.close();
				if(LogGuard.isDebug) {
					for(ListItem s:listItems){
						Log.d(TAG, s.toString());
					}
				}
				
				emptyCategoryName = ctxt.getApplicationContext().getResources()
						.getString(R.string.empty_category);
			}catch (Exception e) {
				Log.e(TAG, "GetShoppingDataTask", e);
			}
			return null;
			
		}
		
		@Override
		protected void onPostExecute(Void v) {
			if(LogGuard.isDebug) Log.d(TAG, "GetShoppingDataTask.onPostExecute" + (listener==null));
			listener.updateShoppingData(shoppingLists, listItems, itemsMap);
		}
		
	}
	
	void addItemToList(Item item) {
		mItems.add(item);
		Collections.sort(mItems);
		if(categoriesMap != null){
			String category = item.getCategory(); 
			if( category == null || category.length() == 0){
				category = emptyCategoryName; 
			}
			if(categoriesMap.containsKey(category)){
				categoriesMap.get(category).add(item);
			}else{
				List<Item> itemList = new ArrayList<Item>();
				itemList.add(item);
				categoriesMap.put(category, itemList);
			}
			
		}
	}
	
	private void buildCategoryItemMap(boolean rebuild){
		if (categoriesMap == null || rebuild){
			categoriesMap = new HashMap<String, List<Item>>();
			for(Item i: mItems){
				String category = i.getCategory(); 
				if( category == null || category.length() == 0){
					category = emptyCategoryName; 
				}
				if(categoriesMap.containsKey(category)){
					categoriesMap.get(category).add(i);
				}else{
					List<Item> itemList = new ArrayList<Item>();
					itemList.add(i);
					categoriesMap.put(category, itemList);
				}
			}
		}
	}
	List<Item> getItems() {
		return mItems;
	}
	
	List<String> getCategory() {
		buildCategoryItemMap(false);
		return new ArrayList<String>(categoriesMap.keySet());
	}
	
	Map<String, List<Item>> getCategoryItemMap(){
		buildCategoryItemMap(false);
		return categoriesMap;
	}

	long insertListItem(ListItem listItem){
		ContentValues values = new ContentValues();
		values.put(ListItem.COLUMN_LIST_ID, listItem.getList_id());
		values.put(ListItem.COLUMN_ITEM_ID, listItem.getItem().getId());
		values.put(ListItem.COLUMN_QTY, listItem.getQty());
		long newRowId = getWritableDatabase().insert(ListItem.TABLE_NAME, null, values);
		
		return newRowId;
	}
	
	long insertItem(Item item){
		ContentValues values = new ContentValues();
		values.put(Item.COLUMN_NAME, item.getName());
		values.put(Item.COLUMN_QTY_TYPE, item.getQty_type());
		values.put(Item.COLUMN_CATEGORY, item.getCategory());
		long newRowId = getWritableDatabase().insert(Item.TABLE_NAME, null, values);
		
		return newRowId;
	}
	
	long insertShoppingList(ShoppingList shoppingList){
		ContentValues values = new ContentValues();
		values.put(ShoppingList.COLUMN_NAME, shoppingList.getName());
		values.put(ShoppingList._ID, shoppingList.getId());
		long newRowId = getWritableDatabase().insert(ShoppingList.TABLE_NAME, null, values);
		
		return newRowId;
	}
	
	void deleteListItem(long id){
		String whereClause = ListItem._ID + " = ?";
		String[] args = {String.valueOf(id)};
		getWritableDatabase().delete(ListItem.TABLE_NAME, whereClause, args);
	}
	
	void deleteItem(long id){
		String whereClause = Item._ID + " = ?";
		String[] args = {String.valueOf(id)};
		getWritableDatabase().delete(Item.TABLE_NAME, whereClause, args);
	}
	
	void deleteShoppingList(long id){
		if(LogGuard.isDebug) Log.d(TAG, "deleteShoppingList: list_id" + id);
		String whereClause = ShoppingList._ID + " = ?";
		String[] args = {String.valueOf(id)};
		getWritableDatabase().delete(ShoppingList.TABLE_NAME, whereClause, args);
	}
	
	void deleteItemsFromShoppingList(long list_id){
		if(LogGuard.isDebug) Log.d(TAG, "deleteItemsFromShoppingList: list_id" + list_id);
		String whereClause = ListItem.COLUMN_LIST_ID + " = ?";
		String[] args = {String.valueOf(list_id)};
		getWritableDatabase().delete(ListItem.TABLE_NAME, whereClause, args);
	}
	
	void deleteDoneItemsFromShoppingList(long list_id){
		if(LogGuard.isDebug) Log.d(TAG, "deleteDoneItemsFromShoppingList: list_id" + list_id);
		String whereClause = ListItem.COLUMN_LIST_ID + " = ?" + " and done=1";
		String[] args = {String.valueOf(list_id)};
		getWritableDatabase().delete(ListItem.TABLE_NAME, whereClause, args);
	}
	
	void updateShoppingList(ShoppingList list){
		if(LogGuard.isDebug) Log.d(TAG, "updateShoppingList: " + list);
		String whereClause = ShoppingList._ID + " = ?";
		String[] args = {String.valueOf(list.getId())};
		ContentValues values = new ContentValues();
		values.put(ShoppingList.COLUMN_NAME, list.getName());
		getWritableDatabase().update(ShoppingList.TABLE_NAME, values, whereClause, args);
	}
	
	void updateListItem(ListItem listItem){
		if(LogGuard.isDebug) Log.d(TAG, "updateListItem: " + listItem);
		String whereClause = ListItem._ID + " = ?";
		String[] args = {String.valueOf(listItem.getId())};
		ContentValues values = new ContentValues();
		values.put(ListItem.COLUMN_QTY, listItem.getQty());
		values.put(ListItem.COLUMN_DONE, listItem.getIsDone());
		getWritableDatabase().update(ListItem.TABLE_NAME, values, whereClause, args);
	}
	
	void updateItem(Item item){
		if(LogGuard.isDebug) Log.d(TAG, "updateItem: " + item);
		String whereClause = Item._ID + " = ?";
		String[] args = {String.valueOf(item.getId())};
		ContentValues values = new ContentValues();
		values.put(Item.COLUMN_NAME, item.getName());
		values.put(Item.COLUMN_QTY_TYPE, item.getQty_type());
		values.put(Item.COLUMN_CATEGORY, item.getCategory());
		getWritableDatabase().update(Item.TABLE_NAME, values, whereClause, args);
	}
	
	void updateItemAsync(String name, String qty_type, long item_id, String category) {
		if(LogGuard.isDebug) Log.d(TAG, "updateItemAsync: item_id="+item_id + ", name="+name
				+ ", qty_type="+qty_type
				+ ", category="+category);
		String oldCategory = itemsMap.get(item_id).getCategory();
		itemsMap.get(item_id).setName(name);
		itemsMap.get(item_id).setQty_type(qty_type);
		itemsMap.get(item_id).setCategory(category);
		Collections.sort(mItems);
		if(!category.equals(oldCategory)){
			//rebuild CategoryItemMap
			buildCategoryItemMap(true);
		}
		ModelFragment.executeAsyncTask(new UpdateItemTask(itemsMap.get(item_id)));
		
	}
	
	private class UpdateItemTask extends AsyncTask<Void, Void, Void>{
		private Item item;
		
		UpdateItemTask(Item item){
			this.item=item;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			updateItem(item);
			return null;
		}
		
	}
	
	void deleteItemAsync(long id) {
		if(LogGuard.isDebug) Log.d(TAG, "deleteItemAsync: id="+id);
		Item item = itemsMap.get(id);
		mItems.remove(item);
		
		if (categoriesMap!=null){
			String category = item.getCategory();
			if( category == null || category.length() == 0){
				category = emptyCategoryName; 
			}
			categoriesMap.get(category).remove(item);
			if(categoriesMap.get(category).size() == 0){
				categoriesMap.remove(category);
			}
		}
		itemsMap.remove(id);
		ModelFragment.executeAsyncTask(new DeleteItemTask(id));
	}
	
	private class DeleteItemTask extends AsyncTask<Void, Void, Void>{
		private long id;
		
		DeleteItemTask(long id){
			this.id=id;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			deleteItem(id);
			return null;
		}

	}	
	
	void insertItemAsync(String name, String qty_type, String category) {
		if(LogGuard.isDebug) Log.d(TAG, "insertItemAsync: name="+name + ", qty_type=" + qty_type);
		Item item = new Item(name, qty_type, category);
		ModelFragment.executeAsyncTask(new InsertItemTask(item));
	}
	
	private class InsertItemTask extends AsyncTask<Void, Void, Void>{
		private Item item;
		private long newRowId;
		
		InsertItemTask(Item item){
			this.item=item;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			newRowId = insertItem(item);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			item.setId(newRowId);
			itemsMap.put(newRowId, item);
			addItemToList(item);
		}
		
	}

	
}
