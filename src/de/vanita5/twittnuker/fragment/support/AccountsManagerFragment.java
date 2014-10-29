package de.vanita5.twittnuker.fragment.support;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.support.SignInActivity;
import de.vanita5.twittnuker.adapter.AccountsAdapter;
import de.vanita5.twittnuker.menu.TwidereMenuInflater;
import de.vanita5.twittnuker.provider.TweetStore.Accounts;

public class AccountsManagerFragment extends BaseSupportListFragment implements LoaderCallbacks<Cursor> {

	private AccountsAdapter mAdapter;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_ADD_ACCOUNT: {
				final Intent intent = new Intent(INTENT_ACTION_TWITTER_LOGIN);
				intent.setClass(getActivity(), SignInActivity.class);
				startActivity(intent);
				break;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, TwidereMenuInflater inflater) {
		inflater.inflate(R.menu.menu_accounts_manager, menu);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		mAdapter = new AccountsAdapter(getActivity());
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View view = inflater.inflate(android.R.layout.list_content, null, false);
		final ListView originalList = (ListView) view.findViewById(android.R.id.list);
		final ViewGroup listContainer = (ViewGroup) originalList.getParent();
		listContainer.removeView(originalList);
		inflater.inflate(R.layout.fragment_custom_tabs, listContainer, true);
		return view;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		final Uri uri = Accounts.CONTENT_URI;
		return new CursorLoader(getActivity(), uri, Accounts.COLUMNS, null, null, Accounts.SORT_POSITION);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mAdapter.changeCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.changeCursor(null);
	}
}