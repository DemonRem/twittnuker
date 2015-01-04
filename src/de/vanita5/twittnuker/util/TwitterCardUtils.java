/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
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

import android.graphics.Point;
import android.support.v4.app.Fragment;

import de.vanita5.twittnuker.model.ParcelableStatus.ParcelableCardEntity;
import de.vanita5.twittnuker.model.ParcelableStatus.ParcelableCardEntity.ParcelableValueItem;

public class TwitterCardUtils {

	private static final TwitterCardFragmentFactory sFactory = TwitterCardFragmentFactory.getInstance();

	public static Fragment createCardFragment(ParcelableCardEntity card) {
		if ("player".equals(card.name)) {
			final Fragment playerFragment = sFactory.createPlayerFragment(card);
			if (playerFragment != null) return playerFragment;
			return TwitterCardFragmentFactory.createGenericPlayerFragment(card);
		} else if ("audio".equals(card.name)) {
			final Fragment playerFragment = sFactory.createAudioFragment(card);
			if (playerFragment != null) return playerFragment;
			return TwitterCardFragmentFactory.createGenericPlayerFragment(card);
		} else if ("animated_gif".equals(card.name)) {
			final Fragment playerFragment = sFactory.createAnimatedGifFragment(card);
			if (playerFragment != null) return playerFragment;
			return TwitterCardFragmentFactory.createGenericPlayerFragment(card);
		}
		return null;
	}


	public static Point getCardSize(ParcelableCardEntity card) {
		final ParcelableValueItem player_width = ParcelableCardEntity.getValue(card, "player_width");
		final ParcelableValueItem player_height = ParcelableCardEntity.getValue(card, "player_height");
		if (player_width != null && player_height != null) {
			final int width = ParseUtils.parseInt(String.valueOf(player_width.value));
			final int height = ParseUtils.parseInt(String.valueOf(player_height.value));
			if (width > 0 && height > 0) {
				return new Point(width, height);
			}
		}
		return null;
	}

	public static boolean isCardSupported(ParcelableCardEntity card) {
		if (card == null) return false;
		return "player".equals(card.name) || "audio".equals(card.name) || "animated_gif".equals(card.name);
	}

}