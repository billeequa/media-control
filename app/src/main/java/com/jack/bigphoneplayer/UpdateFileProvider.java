package com.jack.bigphoneplayer;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Grants the Android installer read access to exactly one private APK. */
public class UpdateFileProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    private void validate(Uri uri) {
        if (!"/update.apk".equals(uri.getPath()) || getContext() == null
            || !(getContext().getPackageName() + ".updates").equals(uri.getAuthority())) {
            throw new IllegalArgumentException("Unknown update file");
        }
    }
    @Override public String getType(Uri uri) { validate(uri); return "application/vnd.android.package-archive"; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        validate(uri);
        if (!"r".equals(mode)) throw new FileNotFoundException("Read access only");
        return ParcelFileDescriptor.open(new File(getContext().getCacheDir(), "updates/update.apk"), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        validate(uri);
        String[] columns = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
        MatrixCursor cursor = new MatrixCursor(columns);
        MatrixCursor.RowBuilder row = cursor.newRow();
        for (String column : columns) {
            if (OpenableColumns.DISPLAY_NAME.equals(column)) row.add("big-phone-player.apk");
            else if (OpenableColumns.SIZE.equals(column)) row.add(new File(getContext().getCacheDir(), "updates/update.apk").length());
            else row.add(null);
        }
        return cursor;
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
