package jp.ne.hatena.hackugyo.procon;

/**
 * 定数を管理します.
 */
public interface Config {

    /**
     * APIのタイムアウト（秒数）
     */
    int HTTP_CLIENT_TIMEOUT_SECOND = 60;

    /****************************************************
     * Shared Preferences *
     ***************************************************/
    /** 共有設定のタグ */
    public static final String SHARED_PREFERENCES = "shared_pref";
}
