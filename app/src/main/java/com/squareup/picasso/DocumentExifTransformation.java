package com.squareup.picasso;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class DocumentExifTransformation implements Transformation {
    private static final String[] CONTENT_ORIENTATION = new String[] {
            MediaStore.Images.ImageColumns.ORIENTATION
    };

    final Context context;
    final Uri uri;

    public DocumentExifTransformation(Context context, Uri uri) {
        this.context = context;
        this.uri = uri;
    }

    @Override public Bitmap transform(Bitmap source) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return source;
        if (!DocumentsContract.isDocumentUri(context, uri)) return source;

        int exifRotation = getExifOrientation(context, uri);
        if (exifRotation != 0) {
            Matrix matrix = new Matrix();
            matrix.preRotate(exifRotation);

            Bitmap rotated =
                    Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
            if (rotated != source) {
                source.recycle();
            }
            return rotated;
        }

        return source;
    }

    @Override public String key() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return "documentTransform()";
        return "documentExifTransform(" + DocumentsContract.getDocumentId(uri) + ")";
    }

    static int getExifOrientation(Context context, Uri uri) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;
        try {
            String id = DocumentsContract.getDocumentId(uri);
            id = id.split(":")[1];
            cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    CONTENT_ORIENTATION, MediaStore.Images.Media._ID + " = ?", new String[] { id }, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return 0;
            }
            return cursor.getInt(0);
        } catch (RuntimeException ignored) {
            // If the orientation column doesn't exist, assume no rotation.
            return 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
