/*
 * Copyright (C) 2018 Peter Gregus for GravityBox Project (C3C076@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ceco.oreo.gravitybox.ledcontrol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.ceco.oreo.gravitybox.ModLedControl;
import com.ceco.oreo.gravitybox.Utils;

import android.app.Notification;
import android.content.SharedPreferences;
import android.os.Bundle;

public class QuietHours {
    public static final String PKG_WEARABLE_APP = "com.google.android.wearable.app";
    public enum Mode { ON, OFF, AUTO, WEAR };

    public static final class SystemSound {
        public static final String DIALPAD = "dialpad";
        public static final String TOUCH = "touch";
        public static final String SCREEN_LOCK = "screen_lock";
        public static final String CHARGER = "charger";
        public static final String RINGER = "ringer";
    }

    private static final List<String> NOTIF_TEXT_FIELDS = new ArrayList<>(Arrays.asList(
            "android.title","android.text","android.subText","android.infoText",
            "android.summaryText","android.bigText"));

    public boolean uncLocked;
    public boolean enabled;
    int start;
    int end;
    int startAlt;
    int endAlt;
    public boolean muteLED;
    public boolean muteVibe;
    public Set<String> muteSystemSounds;
    public boolean showStatusbarIcon;
    public Mode mode;
    public boolean interactive;
    Set<String> weekDays;
    public boolean muteSystemVibe;

    public QuietHours(Bundle prefs) {
        uncLocked = prefs.getBoolean(QuietHoursActivity.EXTRA_QH_LOCKED);
        enabled = prefs.getBoolean(QuietHoursActivity.EXTRA_QH_ENABLED);
        start = prefs.getInt(QuietHoursActivity.EXTRA_QH_START);
        end = prefs.getInt(QuietHoursActivity.EXTRA_QH_END);
        startAlt = prefs.getInt(QuietHoursActivity.EXTRA_QH_START_ALT);
        endAlt = prefs.getInt(QuietHoursActivity.EXTRA_QH_END_ALT);
        muteLED = prefs.getBoolean(QuietHoursActivity.EXTRA_QH_MUTE_LED);
        muteVibe = prefs.getBoolean(QuietHoursActivity.EXTRA_QH_MUTE_VIBE);
        muteSystemSounds = new HashSet<String>(prefs.getStringArrayList(QuietHoursActivity.EXTRA_QH_MUTE_SYSTEM_SOUNDS));
        showStatusbarIcon = prefs.getBoolean(QuietHoursActivity.EXTRA_QH_STATUSBAR_ICON);
        mode = Mode.valueOf(prefs.getString(QuietHoursActivity.EXTRA_QH_MODE));
        interactive = prefs.getBoolean(QuietHoursActivity.EXTRA_QH_INTERACTIVE);
        weekDays = new HashSet<String>(prefs.getStringArrayList(QuietHoursActivity.EXTRA_QH_WEEKDAYS));
        muteSystemVibe = prefs.getBoolean(QuietHoursActivity.EXTRA_QH_MUTE_SYSTEM_VIBE);
    }

    public QuietHours(SharedPreferences prefs) {
        uncLocked = prefs.getBoolean(QuietHoursActivity.PREF_KEY_QH_LOCKED, false);
        enabled = prefs.getBoolean(QuietHoursActivity.PREF_KEY_QH_ENABLED, false);
        start = prefs.getInt(QuietHoursActivity.PREF_KEY_QH_START, 1380);
        end = prefs.getInt(QuietHoursActivity.PREF_KEY_QH_END, 360);
        startAlt = prefs.getInt(QuietHoursActivity.PREF_KEY_QH_START_ALT, 1380);
        endAlt = prefs.getInt(QuietHoursActivity.PREF_KEY_QH_END_ALT, 360);
        muteLED = prefs.getBoolean(QuietHoursActivity.PREF_KEY_QH_MUTE_LED, false);
        muteVibe = prefs.getBoolean(QuietHoursActivity.PREF_KEY_QH_MUTE_VIBE, true);
        muteSystemSounds = prefs.getStringSet(QuietHoursActivity.PREF_KEY_QH_MUTE_SYSTEM_SOUNDS,
                new HashSet<String>());
        showStatusbarIcon = prefs.getBoolean(QuietHoursActivity.PREF_KEY_QH_STATUSBAR_ICON, true);
        mode = Mode.valueOf(prefs.getString(QuietHoursActivity.PREF_KEY_QH_MODE, "AUTO"));
        interactive = prefs.getBoolean(QuietHoursActivity.PREF_KEY_QH_INTERACTIVE, false);
        weekDays = prefs.getStringSet(QuietHoursActivity.PREF_KEY_QH_WEEKDAYS,
                new HashSet<String>(Arrays.asList("2","3","4","5","6")));
        muteSystemVibe = prefs.getBoolean(QuietHoursActivity.PREF_KEY_MUTE_SYSTEM_VIBE, false);
    }

    public boolean quietHoursActive(LedSettings ls, Notification n, boolean userPresent) {
        if (uncLocked || !enabled) return false;

        if (mode == Mode.WEAR) {
            return true;
        }

        if (ls.getEnabled() && ls.getQhIgnore()) {
            if (ls.getQhIgnoreList() == null || ls.getQhIgnoreList().trim().isEmpty()) {
                if (ModLedControl.DEBUG) ModLedControl.log("QH ignored for all notifications");
                return false;
            } else {
                List<CharSequence> notifTexts = getNotificationTexts(n);
                String[] keywords = ls.getQhIgnoreList().trim().split(",");
                boolean ignore = false;
                for (String kw : keywords) {
                    kw = kw.toLowerCase(Locale.getDefault());
                    ignore |= n.tickerText != null && n.tickerText.toString()
                            .toLowerCase(Locale.getDefault()).contains(kw);
                    for (CharSequence notifText : notifTexts) {
                        ignore |= notifText.toString().toLowerCase(Locale.getDefault()).contains(kw);
                    }
                }
                if (ModLedControl.DEBUG) ModLedControl.log("QH ignore list contains keyword?: " + ignore);
                return (ignore ? false : (quietHoursActive() || (interactive && userPresent)));
            }
        } else {
            return (quietHoursActive() || (interactive && userPresent));
        }
    }

    public boolean quietHoursActive() {
        if (uncLocked || !enabled) return false;

        if (mode != Mode.AUTO) {
            return (mode == Mode.ON || mode == Mode.WEAR);
        }

        Calendar c = new GregorianCalendar();
        c.setTimeInMillis(System.currentTimeMillis());
        int curMin = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        int s = start; 
        int e = end;
        if (!weekDays.contains(String.valueOf(dayOfWeek))) {
            s = startAlt;
            e = endAlt;
        }

        // special logic for transition from week day to weekend and vice versa
        // we assume people stay up longer before weekend  
        if (isTransitionToWeekend(dayOfWeek)) {
            if (curMin > end) {
                // we are after previous QH
                if (startAlt > endAlt) {
                    // weekend range spans midnight
                    // let's apply weekend start time instead
                    s = startAlt;
                    if (ModLedControl.DEBUG) ModLedControl.log("Applying weekend start time for day before weekend");
                } else {
                    // weekend range happens on the next day
                    if (ModLedControl.DEBUG) ModLedControl.log("Ignoring quiet hours for day before weekend");
                    return false;
                }
            }
        }
        // we assume people go to sleep earlier before week day
        if (isTransitionToWeekDay(dayOfWeek)) {
            if (curMin > endAlt) {
                // we are after previous QH
                if (start > end) {
                    // weekday range spans midnight
                    // let's apply weekday start time instead
                    s = start;
                    if (ModLedControl.DEBUG) ModLedControl.log("Applying weekday start time for day before weekday");
                } else {
                    // weekday range happens on the next day
                    if (ModLedControl.DEBUG) ModLedControl.log("Ignoring quiet hours for day before weekday");
                    return false;
                }
            }
        }

        return (Utils.isTimeOfDayInRange(System.currentTimeMillis(), s, e));
    }

    public boolean isSystemSoundMuted(String systemSound) {
        return (muteSystemSounds.contains(systemSound) && quietHoursActive());
    }

    private boolean isTransitionToWeekend(int day) {
        int nextDay = (day==7 ? 1 : day+1);
        return (weekDays.contains(String.valueOf(day)) &&
                    !weekDays.contains(String.valueOf(nextDay)));
    }

    private boolean isTransitionToWeekDay(int day) {
        int nextDay = (day==7 ? 1 : day+1);
        return (!weekDays.contains(String.valueOf(day)) &&
                    weekDays.contains(String.valueOf(nextDay)));
    }

    private List<CharSequence> getNotificationTexts(Notification notification) {
        List<CharSequence> texts = new ArrayList<>();

        for (String extra : NOTIF_TEXT_FIELDS) {
            CharSequence cs = notification.extras.getCharSequence(extra);
            if (cs != null) texts.add(cs);
        }

        if (ModLedControl.DEBUG) {
            for (CharSequence text : texts) {
                ModLedControl.log("Notif text: " + text);
            }
        }

        return texts;
    }
}
