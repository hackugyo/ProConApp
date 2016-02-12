package jp.ne.hatena.hackugyo.procon.util;

import android.util.Patterns;
import android.webkit.URLUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * URL文字列の処理関連で誤実装しがちな箇所を切り出したユーティリティ
 */
public class UrlUtils {

    private UrlUtils() {

    }

    /**
     * URLが適切かどうかを返します．{@link URLUtil#isValidUrl(String)}にはバグがあるので使わないでください．
     *
     * @see <a href="http://stackoverflow.com/a/5930532/2338047">参考ページ</a>
     * @param potentialUrl
     * @return valid or not valid
     */
    public static boolean isValidUrl(String potentialUrl) {
        if (potentialUrl == null) return false;
        return Patterns.WEB_URL.matcher(potentialUrl).matches();
    }

    public static String getQueryStringFromParams(HashMap<String, String> params) {
        if (params == null) return null;
        StringBuilder sb = new StringBuilder();

        for (java.util.Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = "";

            if (entry.getValue() == null) {
                LogUtils.e("Cannot read params for " + key + " = null", new NullPointerException());
                continue;
            }
            try {
                value = URLEncoder.encode(entry.getValue(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                LogUtils.e("Cannot encode.", e);
            }
            if (StringUtils.isEmpty(value)) continue;
            if (sb.length() != 0) sb.append("&");
            sb.append(key).append("=").append(value);
        }
        return sb.toString();
    }

    public static boolean isTwitterUrl(String url) {
        return TWITTER_POST_URL.matcher(url).matches();
    }

    private static String TWITTER_DOMAIN_NAME = "(mobile.|)twitter.com";
    public static final Pattern TWITTER_POST_URL = Pattern.compile(
            "((?:(http|https|Http|Https|rtsp|Rtsp):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
                    + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
                    + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
                    + "(?:" + TWITTER_DOMAIN_NAME + ")"
                    + "(?:\\:\\d{1,5})?)" // plus option port number
                    + "(\\/(?:(?:[" + Patterns.GOOD_IRI_CHAR + "\\;\\/\\?\\:\\@\\&\\=\\#\\~"  // plus option query params
                    + "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
                    + "(?:\\b|$)");
}
