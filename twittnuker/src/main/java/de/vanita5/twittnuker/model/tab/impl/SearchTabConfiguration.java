package de.vanita5.twittnuker.model.tab.impl;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.fragment.StatusesSearchFragment;
import de.vanita5.twittnuker.model.Tab;
import de.vanita5.twittnuker.model.tab.DrawableHolder;
import de.vanita5.twittnuker.model.tab.StringHolder;
import de.vanita5.twittnuker.model.tab.TabConfiguration;
import de.vanita5.twittnuker.model.tab.argument.TextQueryArguments;
import de.vanita5.twittnuker.model.tab.conf.StringExtraConfiguration;

import static de.vanita5.twittnuker.constant.IntentConstants.EXTRA_QUERY;

public class SearchTabConfiguration extends TabConfiguration {
    @NonNull
    @Override
    public StringHolder getName() {
        return StringHolder.resource(R.string.search);
    }

    @NonNull
    @Override
    public DrawableHolder getIcon() {
        return DrawableHolder.Builtin.SEARCH;
    }

    @AccountFlags
    @Override
    public int getAccountFlags() {
        return FLAG_HAS_ACCOUNT | FLAG_ACCOUNT_REQUIRED;
    }

    @Nullable
    @Override
    public ExtraConfiguration[] getExtraConfigurations(Context context) {
        return new ExtraConfiguration[]{
                new StringExtraConfiguration(EXTRA_QUERY, null).maxLines(1).title(R.string.search_statuses).headerTitle(R.string.query)
        };
    }

    @Override
    public boolean applyExtraConfigurationTo(@NonNull Tab tab, @NonNull ExtraConfiguration extraConf) {
        final TextQueryArguments arguments = (TextQueryArguments) tab.getArguments();
        assert arguments != null;
        switch (extraConf.getKey()) {
            case EXTRA_QUERY: {
                final String query = ((StringExtraConfiguration) extraConf).getValue();
                if (query == null) return false;
                arguments.setQuery(query);
                break;
            }
        }
        return true;
    }

    @Override
    public boolean readExtraConfigurationFrom(@NonNull Tab tab, @NonNull ExtraConfiguration extraConf) {
        final TextQueryArguments arguments = (TextQueryArguments) tab.getArguments();
        if (arguments == null) return false;
        switch (extraConf.getKey()) {
            case EXTRA_QUERY: {
                ((StringExtraConfiguration) extraConf).setValue(arguments.getQuery());
                break;
            }
        }
        return true;
    }

    @NonNull
    @Override
    public Class<? extends Fragment> getFragmentClass() {
        return StatusesSearchFragment.class;
    }
}