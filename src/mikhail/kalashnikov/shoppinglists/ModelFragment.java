package mikhail.kalashnikov.shoppinglists;

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
	private DataModel model;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		if(LogGuard.isDebug) Log.d(TAG, "onActivityCreated");
		
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);
		model = DataModel.getInstance(getActivity().getApplicationContext());
		uploadData();
	}
	
	synchronized public void uploadData(){
		if(LogGuard.isDebug) Log.d(TAG, "uploadData = " + model.isDataUploaded());
		if (model.isDataUploaded()){
			((ShoppingListsActivity)getActivity()).showListItems(model.getShoppingList());
		}else {
			model.uploadData(this);
		}
	}
	
	
	@Override
	public void updateShoppingData(List<ShoppingList> shoppingLists,
			List<ListItem> listItems, Map<Long, Item> itemsMap) {
		if(LogGuard.isDebug) Log.d(TAG, "updateShoppingData");
		uploadData();
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
	
	public DataModel getModel(){
		return model;
	}
}
	
	
