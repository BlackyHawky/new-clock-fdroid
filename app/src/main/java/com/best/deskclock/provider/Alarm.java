/*
 * Copyright (C) 2013 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.provider;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.loader.content.CursorLoader;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Weekdays;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public final class Alarm implements Parcelable, ClockContract.AlarmsColumns {
    /**
     * Alarms start with an invalid id when it hasn't been saved to the database.
     */
    public static final long INVALID_ID = -1;

    public static final Parcelable.Creator<Alarm> CREATOR = new Parcelable.Creator<>() {
        public Alarm createFromParcel(Parcel p) {
            return new Alarm(p);
        }

        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };
    /**
     * The default sort order for this table
     */
    private static final String DEFAULT_SORT_ORDER =
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + HOUR + ", " +
                    ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + MINUTES + " ASC" + ", " +
                    ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns._ID + " DESC";
    private static final String[] QUERY_COLUMNS = {
            _ID,
            HOUR,
            MINUTES,
            DAYS_OF_WEEK,
            ENABLED,
            STOP_ALARM_WHEN_RINGTONE_ENDS,
            REPEAT_ALARM,
            VIBRATE,
            LABEL,
            RINGTONE,
            DELETE_AFTER_USE,
            INCREASING_VOLUME,
    };
    private static final String[] QUERY_ALARMS_WITH_INSTANCES_COLUMNS = {
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + _ID,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + HOUR,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + MINUTES,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + DAYS_OF_WEEK,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ENABLED,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + STOP_ALARM_WHEN_RINGTONE_ENDS,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + REPEAT_ALARM,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + VIBRATE,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + LABEL,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + RINGTONE,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + DELETE_AFTER_USE,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + INCREASING_VOLUME,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "."
                    + ClockContract.InstancesColumns.ALARM_STATE,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns._ID,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.YEAR,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.MONTH,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.DAY,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.HOUR,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.MINUTES,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.LABEL,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.STOP_ALARM_WHEN_RINGTONE_ENDS,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.REPEAT_ALARM,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.VIBRATE
    };
    /**
     * These save calls to cursor.getColumnIndexOrThrow()
     * THEY MUST BE KEPT IN SYNC WITH ABOVE QUERY COLUMNS
     */
    private static final int ID_INDEX = 0;
    private static final int HOUR_INDEX = 1;
    private static final int MINUTES_INDEX = 2;
    private static final int DAYS_OF_WEEK_INDEX = 3;
    private static final int ENABLED_INDEX = 4;
    private static final int STOP_ALARM_WHEN_RINGTONE_ENDS_INDEX = 5;
    private static final int REPEAT_ALARM_INDEX = 6;
    private static final int VIBRATE_INDEX = 7;
    private static final int LABEL_INDEX = 8;
    private static final int RINGTONE_INDEX = 9;
    private static final int DELETE_AFTER_USE_INDEX = 10;
    private static final int INCREASING_VOLUME_INDEX = 11;

    private static final int INSTANCE_STATE_INDEX = 12;
    public static final int INSTANCE_ID_INDEX = 13;
    public static final int INSTANCE_YEAR_INDEX = 14;
    public static final int INSTANCE_MONTH_INDEX = 15;
    public static final int INSTANCE_DAY_INDEX = 16;
    public static final int INSTANCE_HOUR_INDEX = 17;
    public static final int INSTANCE_MINUTE_INDEX = 18;
    public static final int INSTANCE_LABEL_INDEX = 19;
    public static final int INSTANCE_STOP_ALARM_WHEN_RINGTONE_ENDS_INDEX = 20;
    public static final int INSTANCE_REPEAT_ALARM_INDEX = 21;
    public static final int INSTANCE_VIBRATE_INDEX = 22;

    private static final int COLUMN_COUNT = INCREASING_VOLUME_INDEX + 1;
    private static final int ALARM_JOIN_INSTANCE_COLUMN_COUNT = INSTANCE_VIBRATE_INDEX + 1;
    // Public fields
    public long id;
    public boolean enabled;
    public int hour;
    public int minutes;
    public Weekdays daysOfWeek;
    public boolean stopAlarmWhenRingtoneEnds;
    public boolean repeatAlarm;
    public boolean vibrate;
    public String label;
    public Uri alert;
    public boolean deleteAfterUse;
    public boolean increasingVolume;
    public int instanceState;
    public int instanceId;

    // Creates a default alarm at the current time.
    public Alarm() {
        this(0, 0);
    }

    public Alarm(int hour, int minutes) {
        this.id = INVALID_ID;
        this.hour = hour;
        this.minutes = minutes;
        this.stopAlarmWhenRingtoneEnds = true;
        this.repeatAlarm = true;
        this.vibrate = true;
        this.daysOfWeek = Weekdays.NONE;
        this.label = "";
        this.alert = DataModel.getDataModel().getAlarmRingtoneUriFromSettings();
        this.deleteAfterUse = false;
        this.increasingVolume = false;
    }

    public Alarm(Cursor c) {
        id = c.getLong(ID_INDEX);
        enabled = c.getInt(ENABLED_INDEX) == 1;
        hour = c.getInt(HOUR_INDEX);
        minutes = c.getInt(MINUTES_INDEX);
        daysOfWeek = Weekdays.fromBits(c.getInt(DAYS_OF_WEEK_INDEX));
        stopAlarmWhenRingtoneEnds = c.getInt(STOP_ALARM_WHEN_RINGTONE_ENDS_INDEX) == 1;
        repeatAlarm = c.getInt(REPEAT_ALARM_INDEX) == 1;
        vibrate = c.getInt(VIBRATE_INDEX) == 1;
        label = c.getString(LABEL_INDEX);
        deleteAfterUse = c.getInt(DELETE_AFTER_USE_INDEX) == 1;
        increasingVolume = c.getInt(INCREASING_VOLUME_INDEX) == 1;

        if (c.getColumnCount() == ALARM_JOIN_INSTANCE_COLUMN_COUNT) {
            instanceState = c.getInt(INSTANCE_STATE_INDEX);
            instanceId = c.getInt(INSTANCE_ID_INDEX);
        }

        if (c.isNull(RINGTONE_INDEX)) {
            // Should we be saving this with the current ringtone or leave it null
            // so it changes when user changes default ringtone?
            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        } else {
            alert = Uri.parse(c.getString(RINGTONE_INDEX));
        }
    }

    @SuppressLint("ParcelClassLoader")
    Alarm(Parcel p) {
        id = p.readLong();
        enabled = p.readInt() == 1;
        hour = p.readInt();
        minutes = p.readInt();
        daysOfWeek = Weekdays.fromBits(p.readInt());
        stopAlarmWhenRingtoneEnds = p.readInt() == 1;
        repeatAlarm = p.readInt() == 1;
        vibrate = p.readInt() == 1;
        label = p.readString();
        alert = p.readParcelable(null);
        deleteAfterUse = p.readInt() == 1;
        increasingVolume = p.readInt() == 1;
    }

    public static ContentValues createContentValues(Alarm alarm) {
        ContentValues values = new ContentValues(COLUMN_COUNT);
        if (alarm.id != INVALID_ID) {
            values.put(ClockContract.AlarmsColumns._ID, alarm.id);
        }

        values.put(ENABLED, alarm.enabled ? 1 : 0);
        values.put(HOUR, alarm.hour);
        values.put(MINUTES, alarm.minutes);
        values.put(DAYS_OF_WEEK, alarm.daysOfWeek.getBits());
        values.put(STOP_ALARM_WHEN_RINGTONE_ENDS, alarm.stopAlarmWhenRingtoneEnds ? 1 : 0);
        values.put(REPEAT_ALARM, alarm.repeatAlarm ? 1 : 0);
        values.put(VIBRATE, alarm.vibrate ? 1 : 0);
        values.put(LABEL, alarm.label);
        values.put(DELETE_AFTER_USE, alarm.deleteAfterUse);
        values.put(INCREASING_VOLUME, alarm.increasingVolume ? 1 : 0);
        if (alarm.alert == null) {
            // We want to put null, so default alarm changes
            values.putNull(RINGTONE);
        } else {
            values.put(RINGTONE, alarm.alert.toString());
        }

        return values;
    }

    public static Intent createIntent(Context context, Class<?> cls, long alarmId) {
        return new Intent(context, cls).setData(getContentUri(alarmId));
    }

    public static Uri getContentUri(long alarmId) {
        return ContentUris.withAppendedId(CONTENT_URI, alarmId);
    }

    public static long getId(Uri contentUri) {
        return ContentUris.parseId(contentUri);
    }

    /**
     * Get alarm cursor loader for all alarms.
     *
     * @param context to query the database.
     * @return cursor loader with all the alarms.
     */
    public static CursorLoader getAlarmsCursorLoader(Context context) {
        return new CursorLoader(context, ALARMS_WITH_INSTANCES_URI,
                QUERY_ALARMS_WITH_INSTANCES_COLUMNS, null, null, DEFAULT_SORT_ORDER) {
            @Override
            public Cursor loadInBackground() {
                // Prime the ringtone title cache for later access. Most alarms will refer to
                // system ringtones.
                DataModel.getDataModel().loadRingtoneTitles();

                return super.loadInBackground();
            }
        };
    }

    /**
     * Get alarm by id.
     *
     * @param cr      provides access to the content model
     * @param alarmId for the desired alarm.
     * @return alarm if found, null otherwise
     */
    public static Alarm getAlarm(ContentResolver cr, long alarmId) {
        try (Cursor cursor = cr.query(getContentUri(alarmId), QUERY_COLUMNS, null, null, null)) {
            assert cursor != null;
            if (cursor.moveToFirst()) {
                return new Alarm(cursor);
            }
        }

        return null;
    }

    /**
     * Get all alarms given conditions.
     *
     * @param cr            provides access to the content model
     * @param selection     A filter declaring which rows to return, formatted as an
     *                      SQL WHERE clause (excluding the WHERE itself). Passing null will
     *                      return all rows for the given URI.
     * @param selectionArgs You may include ?s in selection, which will be
     *                      replaced by the values from selectionArgs, in the order that they
     *                      appear in the selection. The values will be bound as Strings.
     * @return list of alarms matching where clause or empty list if none found.
     */
    public static List<Alarm> getAlarms(ContentResolver cr, String selection,
                                        String... selectionArgs) {
        final List<Alarm> result = new LinkedList<>();
        try (Cursor cursor = cr.query(CONTENT_URI, QUERY_COLUMNS, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    result.add(new Alarm(cursor));
                } while (cursor.moveToNext());
            }
        }

        return result;
    }

    public static boolean isTomorrow(Alarm alarm, Calendar now) {
        if (alarm.instanceState == AlarmInstance.SNOOZE_STATE) {
            return false;
        }

        final int totalAlarmMinutes = alarm.hour * 60 + alarm.minutes;
        final int totalNowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return totalAlarmMinutes <= totalNowMinutes;
    }

    public static Alarm addAlarm(ContentResolver contentResolver, Alarm alarm) {
        ContentValues values = createContentValues(alarm);
        Uri uri = contentResolver.insert(CONTENT_URI, values);
        alarm.id = getId(uri);
        return alarm;
    }

    public static void updateAlarm(ContentResolver contentResolver, Alarm alarm) {
        if (alarm.id == Alarm.INVALID_ID) return;
        ContentValues values = createContentValues(alarm);
        contentResolver.update(getContentUri(alarm.id), values, null, null);
    }

    public static boolean deleteAlarm(ContentResolver contentResolver, long alarmId) {
        if (alarmId == INVALID_ID) return false;
        int deletedRows = contentResolver.delete(getContentUri(alarmId), "", null);
        return deletedRows == 1;
    }

    public String getLabelOrDefault(Context context) {
        return label.isEmpty() ? context.getString(R.string.default_label) : label;
    }

    public void writeToParcel(Parcel p, int flags) {
        p.writeLong(id);
        p.writeInt(enabled ? 1 : 0);
        p.writeInt(hour);
        p.writeInt(minutes);
        p.writeInt(daysOfWeek.getBits());
        p.writeInt(stopAlarmWhenRingtoneEnds ? 1 : 0);
        p.writeInt(repeatAlarm ? 1 : 0);
        p.writeInt(vibrate ? 1 : 0);
        p.writeString(label);
        p.writeParcelable(alert, flags);
        p.writeInt(deleteAfterUse ? 1 : 0);
        p.writeInt(increasingVolume ? 1 : 0);
    }

    public int describeContents() {
        return 0;
    }

    public AlarmInstance createInstanceAfter(Calendar time) {
        Calendar nextInstanceTime = getNextAlarmTime(time);
        AlarmInstance result = new AlarmInstance(nextInstanceTime, id);
        result.mStopAlarmWhenRingtoneEnds = stopAlarmWhenRingtoneEnds;
        result.mRepeatAlarm = repeatAlarm;
        result.mVibrate = vibrate;
        result.mLabel = label;
        result.mRingtone = alert;
        result.mIncreasingVolume = increasingVolume;
        return result;
    }

    /**
     * @param currentTime the current time
     * @return previous firing time, or null if this is a one-time alarm.
     */
    public Calendar getPreviousAlarmTime(Calendar currentTime) {
        final Calendar previousInstanceTime = Calendar.getInstance(currentTime.getTimeZone());
        previousInstanceTime.set(Calendar.YEAR, currentTime.get(Calendar.YEAR));
        previousInstanceTime.set(Calendar.MONTH, currentTime.get(Calendar.MONTH));
        previousInstanceTime.set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH));
        previousInstanceTime.set(Calendar.HOUR_OF_DAY, hour);
        previousInstanceTime.set(Calendar.MINUTE, minutes);
        previousInstanceTime.set(Calendar.SECOND, 0);
        previousInstanceTime.set(Calendar.MILLISECOND, 0);

        final int subtractDays = daysOfWeek.getDistanceToPreviousDay(previousInstanceTime);
        if (subtractDays > 0) {
            previousInstanceTime.add(Calendar.DAY_OF_WEEK, -subtractDays);
            return previousInstanceTime;
        } else {
            return null;
        }
    }

    public Calendar getNextAlarmTime(Calendar currentTime) {
        final Calendar nextInstanceTime = Calendar.getInstance(currentTime.getTimeZone());
        nextInstanceTime.set(Calendar.YEAR, currentTime.get(Calendar.YEAR));
        nextInstanceTime.set(Calendar.MONTH, currentTime.get(Calendar.MONTH));
        nextInstanceTime.set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH));
        nextInstanceTime.set(Calendar.HOUR_OF_DAY, hour);
        nextInstanceTime.set(Calendar.MINUTE, minutes);
        nextInstanceTime.set(Calendar.SECOND, 0);
        nextInstanceTime.set(Calendar.MILLISECOND, 0);

        // If we are still behind the passed in currentTime, then add a day
        if (nextInstanceTime.getTimeInMillis() <= currentTime.getTimeInMillis()) {
            nextInstanceTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        // The day of the week might be invalid, so find next valid one
        final int addDays = daysOfWeek.getDistanceToNextDay(nextInstanceTime);
        if (addDays > 0) {
            nextInstanceTime.add(Calendar.DAY_OF_WEEK, addDays);
        }

        // Daylight Savings Time can alter the hours and minutes when adjusting the day above.
        // Reset the desired hour and minute now that the correct day has been chosen.
        nextInstanceTime.set(Calendar.HOUR_OF_DAY, hour);
        nextInstanceTime.set(Calendar.MINUTE, minutes);

        return nextInstanceTime;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof final Alarm other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(id).hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "Alarm{" +
                "alert=" + alert +
                ", id=" + id +
                ", enabled=" + enabled +
                ", hour=" + hour +
                ", minutes=" + minutes +
                ", daysOfWeek=" + daysOfWeek +
                ", stopAlarmWhenRingtoneEnds=" + stopAlarmWhenRingtoneEnds +
                ", repeatAlarm=" + repeatAlarm +
                ", vibrate=" + vibrate +
                ", label='" + label + '\'' +
                ", deleteAfterUse=" + deleteAfterUse +
                ", increasingVolume=" + increasingVolume +
                '}';
    }

    public static Alarm getAlarmByLabel(ContentResolver cr, String label) {
        List<Alarm> alarms = Alarm.getAlarms(cr, LABEL + "=?", label);
        if (!alarms.isEmpty()) {
            return alarms.get(0);
        } else {
            // Alarm with the given label not found
            return null;
        }
    }

}
