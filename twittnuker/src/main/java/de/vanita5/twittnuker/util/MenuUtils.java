/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2016 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2016 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.AccountSelectorActivity;
import de.vanita5.twittnuker.activity.ColorPickerDialogActivity;
import de.vanita5.twittnuker.constant.SharedPreferenceConstants;
import de.vanita5.twittnuker.fragment.AddStatusFilterDialogFragment;
import de.vanita5.twittnuker.fragment.DestroyStatusDialogFragment;
import de.vanita5.twittnuker.graphic.ActionIconDrawable;
import de.vanita5.twittnuker.graphic.PaddingDrawable;
import de.vanita5.twittnuker.menu.FavoriteItemProvider;
import de.vanita5.twittnuker.menu.SupportStatusShareProvider;
import de.vanita5.twittnuker.model.ParcelableCredentials;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.util.ParcelableCredentialsUtils;
import de.vanita5.twittnuker.util.menu.TwidereMenuInfo;

import java.util.List;

public class MenuUtils implements Constants {
    private MenuUtils() {
    }

    public static void setMenuItemAvailability(final Menu menu, final int id, final boolean available) {
        if (menu == null) return;
        final MenuItem item = menu.findItem(id);
        if (item == null) return;
        item.setVisible(available);
        item.setEnabled(available);
    }

    public static void setMenuItemChecked(final Menu menu, final int id, final boolean checked) {
        if (menu == null) return;
        final MenuItem item = menu.findItem(id);
        if (item == null) return;
        item.setChecked(checked);
    }

    public static void setMenuItemIcon(final Menu menu, final int id, @DrawableRes final int icon) {
        if (menu == null) return;
        final MenuItem item = menu.findItem(id);
        if (item == null) return;
        item.setIcon(icon);
    }

    public static void setMenuItemShowAsActionFlags(Menu menu, int id, int flags) {
        if (menu == null) return;
        final MenuItem item = menu.findItem(id);
        if (item == null) return;
        item.setShowAsActionFlags(flags);
        MenuItemCompat.setShowAsAction(item, flags);
    }

    public static void setMenuItemTitle(final Menu menu, final int id, @StringRes final int icon) {
        if (menu == null) return;
        final MenuItem item = menu.findItem(id);
        if (item == null) return;
        item.setTitle(icon);
    }

    public static void addIntentToMenu(final Context context, final Menu menu, final Intent queryIntent) {
        addIntentToMenu(context, menu, queryIntent, Menu.NONE);
    }

    public static void addIntentToMenu(final Context context, final Menu menu, final Intent queryIntent,
                                       final int groupId) {
        if (context == null || menu == null || queryIntent == null) return;
        final PackageManager pm = context.getPackageManager();
        final Resources res = context.getResources();
        final float density = res.getDisplayMetrics().density;
        final int padding = Math.round(density * 4);
        final List<ResolveInfo> activities = pm.queryIntentActivities(queryIntent, 0);
        for (final ResolveInfo info : activities) {
            final Intent intent = new Intent(queryIntent);
            final Drawable icon = info.loadIcon(pm);
            intent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
            final MenuItem item = menu.add(groupId, Menu.NONE, Menu.NONE, info.loadLabel(pm));
            item.setIntent(intent);
            final int iw = icon.getIntrinsicWidth(), ih = icon.getIntrinsicHeight();
            if (iw > 0 && ih > 0) {
                final Drawable iconWithPadding = new PaddingDrawable(icon, padding);
                iconWithPadding.setBounds(0, 0, iw, ih);
                item.setIcon(iconWithPadding);
            } else {
                item.setIcon(icon);
            }
        }
    }

    public static void setupForStatus(@NonNull final Context context,
                                      @NonNull final SharedPreferencesWrapper preferences,
                                      @NonNull final Menu menu,
                                      @NonNull final ParcelableStatus status,
                                      @NonNull final AsyncTwitterWrapper twitter) {
        final ParcelableCredentials account = ParcelableCredentialsUtils.getCredentials(context,
                status.account_key);
        if (account == null) return;
        setupForStatus(context, preferences, menu, status, account, twitter);
    }

    @UiThread
    public static void setupForStatus(@NonNull final Context context,
                                      @NonNull final SharedPreferencesWrapper preferences,
                                      @NonNull final Menu menu,
                                      @NonNull final ParcelableStatus status,
                                      @NonNull final ParcelableCredentials account,
                                      @NonNull final AsyncTwitterWrapper twitter) {
        if (menu instanceof ContextMenu) {
            ((ContextMenu) menu).setHeaderTitle(context.getString(R.string.status_menu_title_format,
                    UserColorNameManager.decideDisplayName(status.user_name,
                            status.user_screen_name, preferences.getBoolean(KEY_NAME_FIRST)),
                    status.text_unescaped));
        }
        final int retweetHighlight = ContextCompat.getColor(context, R.color.highlight_retweet);
        final int favoriteHighlight = ContextCompat.getColor(context, R.color.highlight_favorite);
        final int likeHighlight = ContextCompat.getColor(context, R.color.highlight_like);
        final int loveHighlight = ContextCompat.getColor(context, R.color.highlight_love);
        final boolean isMyRetweet;
        if (twitter.isCreatingRetweet(status.account_key, status.id)) {
            isMyRetweet = true;
        } else if (twitter.isDestroyingStatus(status.account_key, status.id)) {
            isMyRetweet = false;
        } else {
            isMyRetweet = status.retweeted || Utils.isMyRetweet(status);
        }
        final MenuItem delete = menu.findItem(R.id.delete);
        if (delete != null) {
            delete.setVisible(Utils.isMyStatus(status));
        }
        final MenuItem retweet = menu.findItem(R.id.retweet);
        if (retweet != null) {
            ActionIconDrawable.setMenuHighlight(retweet, new TwidereMenuInfo(isMyRetweet, retweetHighlight));
            retweet.setTitle(isMyRetweet ? R.string.cancel_retweet : R.string.retweet);
        }
        final MenuItem favorite = menu.findItem(R.id.favorite);
        boolean isFavorite = false;
        if (favorite != null) {
            if (twitter.isCreatingFavorite(status.account_key, status.id)) {
                isFavorite = true;
            } else if (twitter.isDestroyingFavorite(status.account_key, status.id)) {
                isFavorite = false;
            } else {
                isFavorite = status.is_favorite;
            }
            ActionProvider provider = MenuItemCompat.getActionProvider(favorite);
            final boolean useStar = preferences.getBoolean(SharedPreferenceConstants.KEY_I_WANT_MY_STARS_BACK);
            if (provider instanceof FavoriteItemProvider) {
                ((FavoriteItemProvider) provider).setIsFavorite(favorite, isFavorite);
            } else {
                if (useStar) {
                    final Drawable oldIcon = favorite.getIcon();
                    if (oldIcon instanceof ActionIconDrawable) {
                        final Drawable starIcon = ContextCompat.getDrawable(context, R.drawable.ic_action_star);
                        favorite.setIcon(new ActionIconDrawable(starIcon, ((ActionIconDrawable) oldIcon).getDefaultColor()));
                    } else {
                        favorite.setIcon(R.drawable.ic_action_star);
                    }
                    ActionIconDrawable.setMenuHighlight(favorite, new TwidereMenuInfo(isFavorite, favoriteHighlight));
                } else {
                    ActionIconDrawable.setMenuHighlight(favorite, new TwidereMenuInfo(isFavorite, likeHighlight));
                }
            }
            if (useStar) {
                favorite.setTitle(isFavorite ? R.string.unfavorite : R.string.favorite);
            } else {
                favorite.setTitle(isFavorite ? R.string.undo_like : R.string.like);
            }
        }
        final MenuItem love = menu.findItem(R.id.love);
        if (love != null) {
            ActionIconDrawable.setMenuHighlight(love, new TwidereMenuInfo(isMyRetweet && status.is_favorite, loveHighlight));
            love.setTitle(isMyRetweet && (favorite != null ? isFavorite : status.is_favorite) ? R.string.undo_love : R.string.love);
        }
        final MenuItem translate = menu.findItem(R.id.translate);
        if (translate != null) {
            final boolean isOfficialKey = Utils.isOfficialCredentials(context, account);
            final SharedPreferencesWrapper prefs = SharedPreferencesWrapper.getInstance(context, SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
            setMenuItemAvailability(menu, R.id.translate, isOfficialKey);
        }
        final MenuItem shareItem = menu.findItem(R.id.share);
        final ActionProvider shareProvider = MenuItemCompat.getActionProvider(shareItem);
        if (shareProvider instanceof SupportStatusShareProvider) {
            ((SupportStatusShareProvider) shareProvider).setStatus(status);
        } else if (shareProvider instanceof ShareActionProvider) {
            final Intent shareIntent = Utils.createStatusShareIntent(context, status);
            ((ShareActionProvider) shareProvider).setShareIntent(shareIntent);
        } else if (shareItem.hasSubMenu()) {
            final Menu shareSubMenu = shareItem.getSubMenu();
            final Intent shareIntent = Utils.createStatusShareIntent(context, status);
            shareSubMenu.removeGroup(Constants.MENU_GROUP_STATUS_SHARE);
            addIntentToMenu(context, shareSubMenu, shareIntent, Constants.MENU_GROUP_STATUS_SHARE);
        } else {
            final Intent shareIntent = Utils.createStatusShareIntent(context, status);
            final Intent chooserIntent = Intent.createChooser(shareIntent, context.getString(R.string.share_status));
            Utils.addCopyLinkIntent(context, chooserIntent, LinkCreator.getStatusWebLink(status));
            shareItem.setIntent(chooserIntent);
        }

    }

    public static boolean handleStatusClick(@NonNull final Context context,
                                            @Nullable final Fragment fragment,
                                            @NonNull final FragmentManager fm,
                                            @NonNull final UserColorNameManager colorNameManager,
                                            @NonNull final AsyncTwitterWrapper twitter,
                                            @NonNull final ParcelableStatus status,
                                            @NonNull final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.copy: {
                if (ClipboardUtils.setText(context, status.text_plain)) {
                    Utils.showOkMessage(context, R.string.text_copied, false);
                }
                break;
            }
            case R.id.retweet: {
                Utils.retweet(status, twitter);
                break;
            }
            case R.id.quote: {
                final Intent intent = new Intent(INTENT_ACTION_QUOTE);
                intent.putExtra(EXTRA_STATUS, status);
                context.startActivity(intent);
                break;
            }
            case R.id.reply: {
                final Intent intent = new Intent(INTENT_ACTION_REPLY);
                intent.putExtra(EXTRA_STATUS, status);
                context.startActivity(intent);
                break;
            }
            case R.id.favorite: {
                Utils.favorite(status, twitter, item);
                break;
            }
            case R.id.delete: {
                DestroyStatusDialogFragment.show(fm, status);
                break;
            }
            case R.id.add_to_filter: {
                AddStatusFilterDialogFragment.show(fm, status);
                break;
            }
            case R.id.love: {
                Utils.retweet(status, twitter);
                Utils.favorite(status, twitter, item);
                break;
            }
            case R.id.set_color: {
                final Intent intent = new Intent(context, ColorPickerDialogActivity.class);
                final int color = colorNameManager.getUserColor(status.user_key);
                if (color != 0) {
                    intent.putExtra(EXTRA_COLOR, color);
                }
                intent.putExtra(EXTRA_CLEAR_BUTTON, color != 0);
                intent.putExtra(EXTRA_ALPHA_SLIDER, false);
                if (fragment != null) {
                    fragment.startActivityForResult(intent, REQUEST_SET_COLOR);
                } else if (context instanceof Activity) {
                    ((Activity) context).startActivityForResult(intent, REQUEST_SET_COLOR);
                }
                break;
            }
            case R.id.open_with_account: {
                final Intent intent = new Intent(INTENT_ACTION_SELECT_ACCOUNT);
                intent.setClass(context, AccountSelectorActivity.class);
                intent.putExtra(EXTRA_SINGLE_SELECTION, true);
                intent.putExtra(EXTRA_ACCOUNT_HOST, status.user_key.getHost());
                if (fragment != null) {
                    fragment.startActivityForResult(intent, REQUEST_SELECT_ACCOUNT);
                } else if (context instanceof Activity) {
                    ((Activity) context).startActivityForResult(intent, REQUEST_SELECT_ACCOUNT);
                }
                break;
            }
            case R.id.open_in_browser: {
                final Uri uri = LinkCreator.getStatusWebLink(status);
                final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setPackage(IntentUtils.getDefaultBrowserPackage(context, uri));
//                IntentSupport.setSelector(intent, new Intent(Intent.ACTION_VIEW).addCategory(IntentSupport.CATEGORY_APP_BROWSER));
                context.startActivity(intent);
                break;
            }
            default: {
                if (item.getIntent() != null) {
                    try {
                        context.startActivity(item.getIntent());
                    } catch (final ActivityNotFoundException e) {
                        Log.w(LOGTAG, e);
                        return false;
                    }
                }
                break;
            }
        }
        return true;
    }
}