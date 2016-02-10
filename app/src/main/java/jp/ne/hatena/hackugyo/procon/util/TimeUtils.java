package jp.ne.hatena.hackugyo.procon.util;


import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.IllegalFieldValueException;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;

import jp.ne.hatena.hackugyo.procon.AppApplication;

public class TimeUtils {
    public static final int SECOND = 1000;
    public static final int MINUTE = 60 * SECOND;
    public static final int HOUR = 60 * MINUTE;
    public static final int DAY = 24 * HOUR;

    private static final SimpleDateFormat[] ACCEPTED_TIMESTAMP_FORMATS = {
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
            new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z", Locale.US)
    };

    private static final SimpleDateFormat VALID_IFMODIFIEDSINCE_FORMAT =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

    public static Date parseTimestamp(String timestamp) {
        for (SimpleDateFormat format : ACCEPTED_TIMESTAMP_FORMATS) {
            // TODO: We shouldn't be forcing the time zone when parsing dates.
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            try {
                return format.parse(timestamp);
            } catch (ParseException ex) {
                continue;
            }
        }

        // All attempts to parse have failed
        return null;
    }

    public static boolean isValidFormatForIfModifiedSinceHeader(String timestamp) {
        try {
            return VALID_IFMODIFIEDSINCE_FORMAT.parse(timestamp)!=null;
        } catch (Exception ex) {
            return false;
        }
    }

    public static long timestampToMillis(String timestamp, long defaultValue) {
        if (TextUtils.isEmpty(timestamp)) {
            return defaultValue;
        }
        Date d = parseTimestamp(timestamp);
        return d == null ? defaultValue : d.getTime();
    }

    /**
     * Format a {@code date} honoring the app preference for using Conference or device timezone.
     * {@code Context} is used to lookup the shared preference settings.
     */
    public static String formatShortDate(Context context, Date date) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        return DateUtils.formatDateRange(context, formatter, date.getTime(), date.getTime(),
                DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_NO_YEAR,
                AppApplication.getDisplayTimeZone(context).getID()).toString();
    }

    public static String formatShortTime(Context context, Date time) {
        // Android DateFormatter will honor the user's current settings.
        DateFormat format = android.text.format.DateFormat.getTimeFormat(context);
        // Override with Timezone based on settings since users can override their phone's timezone
        // with Pacific time zones.
        TimeZone tz = AppApplication.getDisplayTimeZone(context);
        if (tz != null) {
            format.setTimeZone(tz);
        }
        return format.format(time);
    }

    public static String dateStringFromDateTime(DateTime dateTime) {
        return stringFromDateTime(dateTime, "yyyy/MM/dd(E)");
    }

    /**
     *
     * @param dateTime
     * @param formatPattern "yyyy/MM/dd(E)"など
     * @return 指定されたフォーマットの日付文字列、もしくは空文字
     */
    public static String stringFromDateTime(DateTime dateTime, String formatPattern) {
        if (dateTime == null) return "";
        DateTimeFormatter dtf = DateTimeFormat.forPattern(formatPattern).withLocale(Locale.JAPAN);
        return dtf.print(dateTime);
    }

    public static boolean isStrictDaysBetweenPlus(DateTime var0, DateTime var1) {
        if (var0 == null || var1 == null) return false;
        return strictDaysBetween(var0, var1).getDays() > 0;
    }


    public static boolean isStrictDaysBetweenMinus(DateTime var0, DateTime var1) {
        if (var0 == null || var1 == null) return false;
        return strictDaysBetween(var0, var1).getDays() < 0;
    }

    /**
     *
     * @param var0 date from
     * @param var1 date to
     * @return days
     * @see <a href="http://stackoverflow.com/a/17959137">参考リンク</a>
     */
    public static Days strictDaysBetween(DateTime var0, DateTime var1) {
        return Days.daysBetween(var0.toLocalDate(), var1.toLocalDate());
    }

    /**
     * 「201601012700」のように、24時間表記ではなく27時間表記など（テレビ的時間表記，日本独自）になっている場合に，
     * 「201601020300」と正しく解釈して返します．
     * @param dateString
     * @param yyyyMMddHHmm
     * @param tvHourLimit 認識する表記の上限を返します．27時59分が最大であれば，"27"と設定してください．
     * @return
     * @throws IllegalFieldValueException
     */
    public static DateTime parse(String dateString, DateTimeFormatter yyyyMMddHHmm, String tvHourLimit) throws IllegalFieldValueException {
        try {
            return DateTime.parse(dateString, yyyyMMddHHmm);
        } catch  (IllegalFieldValueException e) {
            if (dateString.length() == 12) {
                int tvHour = Integer.valueOf(dateString.substring(8, 10));
                int limit = Integer.valueOf(tvHourLimit);
                if (tvHour <= limit) {
                    int hour = tvHour - 24;
                    final DateTimeFormatter yyyyMMdd = DateTimeFormat.forPattern("yyyyMMdd").withLocale(yyyyMMddHHmm.getLocale());
                    DateTime parsed = DateTime.parse(dateString.substring(0, 8), yyyyMMdd);
                    return parsed.plusHours(hour)
                            .plusMinutes(Integer.valueOf(dateString.substring(10,12)));
                }
            }
            throw e;
        }
    }
}
