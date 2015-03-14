package de.vanita5.twittnuker.activity.support;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.fragment.iface.IBaseFragment;
import de.vanita5.twittnuker.fragment.support.AccountsManagerFragment;

public class AccountsManagerActivity extends BaseActionBarActivity {

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case MENU_HOME: {
				onBackPressed();
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		setContentView(R.layout.activity_content_fragment);
		final FragmentManager fm = getSupportFragmentManager();
		final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.main_content, new AccountsManagerFragment());
		ft.commit();
	}

	@Override
	public void fitSystemWindows(Rect insets) {
		super.fitSystemWindows(insets);
		final FragmentManager fm = getSupportFragmentManager();
        final Fragment f = fm.findFragmentById(R.id.main_content);
		if (f instanceof IBaseFragment) {
			((IBaseFragment) f).requestFitSystemWindows();
		}
	}
}