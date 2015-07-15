/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;

import com.db.chart.model.BarSet;
import com.db.chart.model.ChartSet;

import org.mariotaku.querybuilder.Expression;
import de.vanita5.twittnuker.provider.TwidereDataStore.NetworkUsages;
import de.vanita5.twittnuker.util.MathUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class NetworkUsageInfo {
    private final double[][] chartUsage;
    private final double totalSent, totalReceived;
    private final double[] usageTotal;
    private final double dayUsageMax;
    private final int dayMin;
    private final int dayMax;

    public NetworkUsageInfo(double[][] chartUsage, double[] usageTotal, double totalReceived, double totalSent, double dayUsageMax, int dayMin, int dayMax) {
        this.chartUsage = chartUsage;
        this.usageTotal = usageTotal;
        this.totalReceived = totalReceived;
        this.totalSent = totalSent;
        this.dayUsageMax = dayUsageMax;
        this.dayMin = dayMin;
        this.dayMax = dayMax;
    }

    public static NetworkUsageInfo get(Context context, Date start, Date end, int dayMin, int dayMax) {
        final ContentResolver cr = context.getContentResolver();
        final long startTime = TimeUnit.HOURS.convert(start.getTime(), TimeUnit.MILLISECONDS);
        final long endTime = TimeUnit.HOURS.convert(end.getTime(), TimeUnit.MILLISECONDS);
        final Expression where = Expression.and(Expression.greaterEquals(NetworkUsages.TIME_IN_HOURS, startTime),
                Expression.lesserThan(NetworkUsages.TIME_IN_HOURS, endTime));
        final int days = (int) TimeUnit.DAYS.convert(endTime - startTime, TimeUnit.HOURS);
        final Cursor c = cr.query(NetworkUsages.CONTENT_URI, NetworkUsages.COLUMNS,
                where.getSQL(), null, NetworkUsages.TIME_IN_HOURS);
        final int idxDate = c.getColumnIndex(NetworkUsages.TIME_IN_HOURS);
        final int idxSent = c.getColumnIndex(NetworkUsages.KILOBYTES_SENT);
        final int idxReceived = c.getColumnIndex(NetworkUsages.KILOBYTES_RECEIVED);
        final int idxType = c.getColumnIndex(NetworkUsages.REQUEST_TYPE);
        final double[][] chartUsage = new double[days][RequestType.values().length];
        double totalReceived = 0, totalSent = 0;
        final double[] usageTotal = new double[RequestType.values().length];
        c.moveToFirst();
        while (!c.isAfterLast()) {
            final long hours = c.getLong(idxDate);
            final int idx = (int) TimeUnit.DAYS.convert((hours - startTime), TimeUnit.HOURS);
            final double sent = c.getDouble(idxSent);
            final double received = c.getDouble(idxReceived);
            final String type = c.getString(idxType);
            final double hourTypeTotal = sent + received;
            final int typeIdx = RequestType.getValue(type);
            chartUsage[idx][typeIdx] += hourTypeTotal;
            totalReceived += received;
            totalSent += sent;
            usageTotal[typeIdx] += hourTypeTotal;
            c.moveToNext();
        }
        c.close();

        final BarSet apiSet = new BarSet();
        final BarSet mediaSet = new BarSet();
        final BarSet usageStatisticsSet = new BarSet();

        double dayUsageMax = 0;
        for (int i = 0; i < days; i++) {
            String day = String.valueOf(i + 1);
            final double[] dayUsage = chartUsage[i];
            apiSet.addBar(day, (float) dayUsage[RequestType.API.getValue()]);
            mediaSet.addBar(day, (float) dayUsage[RequestType.MEDIA.getValue()]);
            usageStatisticsSet.addBar(day, (float) dayUsage[RequestType.USAGE_STATISTICS.getValue()]);
            dayUsageMax = Math.max(dayUsageMax, MathUtils.sum(dayUsage));
        }

        apiSet.setColor(Color.RED);
        mediaSet.setColor(Color.GREEN);
        usageStatisticsSet.setColor(Color.BLUE);

        final ArrayList<ChartSet> data = new ArrayList<>();
        data.add(apiSet);
        data.add(mediaSet);
        data.add(usageStatisticsSet);
        return new NetworkUsageInfo(chartUsage, usageTotal, totalReceived, totalSent, dayUsageMax, dayMin, dayMax);
    }

    public int getDayMax() {
        return dayMax;
    }

    public int getDayMin() {
        return dayMin;
    }

    public double getDayUsageMax() {
        return dayUsageMax;
    }

    public double getTotalSent() {
        return totalSent;
    }

    public double[] getUsageTotal() {
        return usageTotal;
    }

    public double getTotalReceived() {
        return totalReceived;
    }

    public double[][] getChartUsage() {
        return chartUsage;
    }
}