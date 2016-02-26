package jp.ne.hatena.hackugyo.procon.model;

/**
 * 中間テーブルのモデルであることを表現する宣言的インタフェース。
 */
interface MiddleModel {
    long getIdForFirst();
    long getIdForSecond();
}
