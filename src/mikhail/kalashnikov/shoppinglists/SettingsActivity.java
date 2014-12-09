package mikhail.kalashnikov.shoppinglists;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
	final static String KEY_PREF_USE_CATEGORY = "pref_use_category";
	final static String KEY_PREF_USE_CATEGORY_ON_MAIN_SCREEN = "pref_category_main_screen";
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}
