package jp.ne.hatena.hackugyo.procon.util;

import android.content.ContentProvider;
import android.net.Uri;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.text.ParseException;
import java.util.regex.Pattern;

import jp.ne.hatena.hackugyo.procon.io.JsonHandler;

/**
 * Various utility methods used by {@link JsonHandler}.
 */
public class ParserUtils {
    /** Used to sanitize a string to be {@link Uri} safe. */
    private static final Pattern sSanitizePattern = Pattern.compile("[^a-z0-9-_]");

    /**
     * Sanitize the given string to be {@link Uri} safe for building
     * {@link ContentProvider} paths.
     */
    public static String sanitizeId(String input) {
        if (input == null) {
            return null;
        }
        return sSanitizePattern.matcher(input.replace("+", "plus").toLowerCase()).replaceAll("");
    }

    /**
     * Parse the given string as a RFC 3339 timestamp, returning the value as
     * milliseconds since the epoch.
     * @see <a href="http://stackoverflow.com/a/7011259">参考リンク</a>
     */
    public static long parseTime(String timestamp) throws ParseException {
        DateTimeFormatter dateFormatter = ISODateTimeFormat.dateTime();
        DateTime dateTime = dateFormatter.parseDateTime(timestamp);
        return dateTime.getMillis();
    }
}

