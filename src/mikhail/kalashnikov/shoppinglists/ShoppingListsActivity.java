package mikhail.kalashnikov.shoppinglists;

import java.util.List;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

public class ShoppingListsActivity extends SherlockFragmentActivity
		implements AddNewListDialog.AddNewListDialogListener, TabListener{
	private final String TAG = getClass().getSimpleName();
	private static final String MODEL="model";
	private ModelFragment model=null;
	private ActionBar bar;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(LogGuard.isDebug) Log.d(TAG, "onCreate getSupportFragmentManager().findFragmentByTag(MODEL)==null " + (getSupportFragmentManager().findFragmentByTag(MODEL)==null));
        if (getSupportFragmentManager().findFragmentByTag(MODEL)==null) {
			model = new ModelFragment();
			getSupportFragmentManager().beginTransaction()
				.add(model, MODEL)
				.commit();
		}else{
			model = (ModelFragment)getSupportFragmentManager().findFragmentByTag(MODEL);
		}
        
        setContentView(R.layout.main);
        
        bar=getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        if(LogGuard.isDebug) Log.d(TAG, "onCreate End");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getSupportMenuInflater().inflate(R.menu.actions, menu);
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()){
    		case R.id.mi_add_list:
    			AddNewListDialog addNewlListDialog = AddNewListDialog.newInstance(null, AddNewListDialog.NEW_LIST_ID);
    			addNewlListDialog.show(getSupportFragmentManager(), "AddNewListDialog");
    			return true;
    		case R.id.mi_edit_list:
    			addNewlListDialog = AddNewListDialog.newInstance(
    					bar.getSelectedTab().getText().toString(),
    					(Long)bar.getSelectedTab().getTag());
    			addNewlListDialog.show(getSupportFragmentManager(), "AddNewListDialog");
    			return true;
    		case R.id.mi_del_list:
    			SherlockDialogFragment dialog = new SherlockDialogFragment(){
    				@Override
    				public Dialog onCreateDialog(Bundle savedInstanceState) {
    					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    					builder.setMessage(R.string.delete_list_confirmation)
    						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									model.getModel().deleteShoppingListWithItemsAsync((Long)bar.getSelectedTab().getTag());
									bar.removeTabAt(bar.getSelectedNavigationIndex());
									
								}
							})
    						.setNegativeButton(android.R.string.cancel, null);
    					return builder.create();
    				}
    			};
    			dialog.show(getSupportFragmentManager(), "DeleteList");
    			return true;
    			
    		case R.id.mi_edit_items:
    			Intent i = new Intent(this, EditItemFragmentActivity.class);
    			startActivity(i);
    			return true;
    		case R.id.mi_settings:
    			startActivity(new Intent(this, SettingsActivity.class));
    			return true;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    void showListItems(List<ShoppingList> shoppingLists){
    	Log.d(TAG, "showListItems model.isDataUploaded()=" + model.getModel().isDataUploaded());
    	findViewById(R.id.progressBar1).setVisibility(View.GONE);
        for (ShoppingList sl: shoppingLists) {
            bar.addTab(bar.newTab().setText(sl.getName())
                        .setTabListener(this).setTag(sl.getId()));
        }

    }

	@Override
	public void onListAdded(String name) {
		long id = model.getModel().insertShoppingListAsync(name);
		bar.addTab(bar.newTab().setText(name)
                .setTabListener(this).setTag(id));
		bar.setSelectedNavigationItem(model.getModel().getShoppingList().size()-1);
		
	}

	@Override
	public void onListEdited(String name, long id) {
		model.getModel().updateShoppingListAsync(name, id);
		bar.getSelectedTab().setText(name);
	}
    
    @Override
    protected void onStart() {
    	if(LogGuard.isDebug) Log.d(TAG, "onStart");
    	super.onStart();
    }
    
    @Override
    protected void onResume() {
    	if(LogGuard.isDebug) Log.d(TAG, "onResume");
    	super.onResume();
    }

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		Log.d(TAG, "onTabSelected model.isDataUploaded()=" + model.getModel().isDataUploaded());
		long id=((Long) tab.getTag()).longValue();

	    ft.replace(android.R.id.content,
	    		ListItemsFragment.newInstance(id));
		
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		// unused	
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// unused
	}

	
}
