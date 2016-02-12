package jp.ne.hatena.hackugyo.procon.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ArrayUtils {

    private ArrayUtils() {

    }

    /**
     * ArrayListインスタンスそのものは変更せず，中身だけをinitialArrayで完全に上書きします．
     *
     * @param targetArray
     * @param initialArray
     * @return targetArray
     */
    public static <T> ArrayList<T> initArrayListWith(ArrayList<T> targetArray, ArrayList<T> initialArray) {
        if (targetArray == null) {
            LogUtils.v("  targetArray is Null. create new one( and return it).");
            targetArray = new ArrayList<T>();
        }
        // 同じインスタンスだった場合，前者をclearしてから後者を入れると，けっきょくぜんぶclearされてしまうので．
        if (targetArray == initialArray) return targetArray;

        targetArray.clear();
        if (initialArray != null) targetArray.addAll(initialArray);
        return targetArray;
    }

    /**
     * 第1引数に第2引数を連結します．第1引数は破壊的に更新されます（インスタンスの参照を変えません）．
     *
     * @param targetArray
     * @param initialArray
     * @return 第1引数と同じインスタンス
     */
    public static <T> List<T> concatList(List<T> targetArray, List<T> initialArray) {
        if (targetArray == null) {
            LogUtils.v("  targetArray is Null. create new one( and return it).");
            targetArray = new ArrayList<T>();
        }
        if (initialArray != null) targetArray.addAll(initialArray);
        return targetArray;
    }

    /**
     *
     * @param first
     * @param second
     * @return 結合した配列（引数の両方ともnullでない場合のみ）．どちらか片方でもnullであれば，null
     */
    public static <T> T[] concat(T[] first, T[] second) {
        if (first == null || second == null) return null;
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static boolean containTrue(boolean[] array) {
        for (boolean value : array) {
            if (value) return true;
        }
        return false;
    }

    /**
     * {@link Arrays#asList(Object...)}にint[]を渡した場合，期待したとおりのArrayListにならないので，<br>
     * 代わりにこのメソッドを使ってください．
     *
     * @see <a
     *      href="http://d.hatena.ne.jp/idesaku/20081021/1224600600">参考ページ</a>
     * @param intArray
     * @return ArrayList
     */
    public static ArrayList<Integer> asList(int[] intArray) {
        if (intArray == null) return null;
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i : intArray) {
            list.add(i);
        }
        return list;
    }

    /**
     * {@link Arrays#asList(Object...)}にlong[]を渡した場合，期待したとおりのArrayListにならないので，<br>
     * 代わりにこのメソッドを使ってください．
     *
     * @see <a
     *      href="http://d.hatena.ne.jp/idesaku/20081021/1224600600">参考ページ</a>
     * @param longArray
     * @return ArrayList
     */
    public static ArrayList<Long> asList(long[] longArray) {
        if (longArray == null) return null;
        ArrayList<Long> list = new ArrayList<Long>();
        for (long i : longArray) {
            list.add(i);
        }
        return list;
    }

    /**
     * {@link Arrays#asList(Object...)}にdouble[]を渡した場合，期待したとおりのArrayListにならないので，<br>
     * 代わりにこのメソッドを使ってください．
     *
     * @see <a
     *      href="http://d.hatena.ne.jp/idesaku/20081021/1224600600">参考ページ</a>
     * @param doubleArray
     * @return ArrayList
     */
    public static ArrayList<Double> asList(double[] doubleArray) {
        if (doubleArray == null) return null;
        ArrayList<Double> list = new ArrayList<Double>();
        for (double i : doubleArray) {
            list.add(i);
        }
        return list;
    }

    /**
     * {@link Arrays#asList(Object...)}にfloat[]を渡した場合，期待したとおりのArrayListにならないので，<br>
     * 代わりにこのメソッドを使ってください．
     *
     * @see <a
     *      href="http://d.hatena.ne.jp/idesaku/20081021/1224600600">参考ページ</a>
     * @param floatArray
     * @return ArrayList
     */
    public static ArrayList<Float> asList(float[] floatArray) {
        if (floatArray == null) return null;
        ArrayList<Float> list = new ArrayList<Float>();
        for (float i : floatArray) {
            list.add(i);
        }
        return list;
    }

    /**
     * {@link Arrays#asList(Object...)}
     * にboolean[]を渡した場合，期待したとおりのArrayListにならないので，<br>
     * 代わりにこのメソッドを使ってください．
     *
     * @see <a
     *      href="http://d.hatena.ne.jp/idesaku/20081021/1224600600">参考ページ</a>
     * @param booleanArray
     * @return ArrayList
     */
    public static ArrayList<Boolean> asList(boolean[] booleanArray) {
        if (booleanArray == null) return null;
        ArrayList<Boolean> list = new ArrayList<Boolean>();
        for (boolean i : booleanArray) {
            list.add(i);
        }
        return list;
    }

    /**
     * {@link ArrayUtils#asList(int[])}を定義したので，可変長引数の
     * {@link Arrays#asList(Object...)} を使わずにすむよう，<br>
     * 同内容メソッドを定義しています．
     *
     * @param array
     * @return ArrayList
     */
    public static <T> ArrayList<T> asList(T[] array) {
        if (array == null) return null;
        return new ArrayList<T>(Arrays.asList(array));
    }

    public static <T>  ArrayList<T> asList(List<? extends T> list) {
        if (list == null) return null;
        return new ArrayList<T>(list);
    }

    public static <T> ArrayList<T> asList(Collection<? extends  T> set) {
        if (set == null) return null;
        ArrayList<T> arrayList = new ArrayList<>();
        arrayList.addAll(set);
        return arrayList;
    }

    public static boolean any(List<?> list) {
        return list != null && !list.isEmpty();
    }

    public static <T> void replaceAll(List<T> target,  List<T> list) {
        if (list == null) throw new NullPointerException();
        if (target == null) target = new ArrayList<T>();
        if (target == list) return;
        target.clear();
        for (T obj : list) {
            target.add(obj);
        }
        return;
    }

    public static <T> ArrayList<T> copy(List<T> original) {
        ArrayList<T> result = new ArrayList<T>();
        if (original != null) result.addAll(original);
        return result;
    }

    public static <T> ArrayList<T> reverse(List<T> origin) {
        ArrayList<T> result = new ArrayList<>(origin);
        Collections.reverse(result);
        return result;
    }

    public static String[] asArray(List<String> list) {
        String[] ts = new String[list.size()];
        return list.toArray(ts);
    }
}
