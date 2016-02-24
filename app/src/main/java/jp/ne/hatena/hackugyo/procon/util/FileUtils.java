package jp.ne.hatena.hackugyo.procon.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by kwatanabe on 16/02/24.
 */
public class FileUtils {
    private FileUtils() {

    }

    public static void saveFile(Context context, String fileName, String str) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fileOutputStream.write(str.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readFile(Context context, String fileName) {
        FileInputStream fileInputStream;
        String text = null;

        try {
            fileInputStream = context.openFileInput(fileName);
            String lineBuffer = null;

            BufferedReader reader= new BufferedReader(new InputStreamReader(fileInputStream,"UTF-8"));
            while( (lineBuffer = reader.readLine()) != null ) {
                text = lineBuffer ;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return text;
    }
}
