package snoof.widget.photo;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class PhotoPickerWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "PhotoWidgetProvider";
    private static final String PREFS_NAME = "PhotoWidgetPrefs";
    private static final String PREF_RADIUS_KEY = "radius_percent_";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        File imgDir = context.getDir("imgDir", Context.MODE_PRIVATE);

        for (int appWidgetId : appWidgetIds) {
            // Delete image file
            File file = new File(imgDir, "pic_" + appWidgetId + ".png");
            if (file.exists()) file.delete();

            // Delete shared preference
            prefs.edit().remove(PREF_RADIUS_KEY + appWidgetId).apply();
            Log.d(TAG, "Cleaned up ID: " + appWidgetId);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.photowidget);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int radiusPercent = prefs.getInt(PREF_RADIUS_KEY + appWidgetId, 50);

        Bitmap photoBitmap = loadImageFromInternalStorage(context, appWidgetId, radiusPercent);

        if (photoBitmap != null) {
            views.setImageViewBitmap(R.id.photoWidgetview, photoBitmap);
        } else {
            views.setImageViewResource(R.id.photoWidgetview, R.drawable.pfptest);
        }

        // Prevents app opening when clicking the widget (per user request)
        Intent emptyIntent = new Intent("snoof.widget.photo.ACTION_DO_NOTHING");
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                emptyIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        views.setOnClickPendingIntent(R.id.photoWidgetview, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static Bitmap loadImageFromInternalStorage(Context context, int appWidgetId, int radiusPercent) {
        File imgFile = new File(context.getDir("imgDir", Context.MODE_PRIVATE), "pic_" + appWidgetId + ".png");

        if (!imgFile.exists()) return null;

        try (FileInputStream fis = new FileInputStream(imgFile)) {
            Bitmap original = BitmapFactory.decodeStream(fis);
            if (original != null) {
                // Dynamic radius calculation for the widget
                float maxRadius = Math.min(original.getWidth(), original.getHeight()) / 2.0f;
                float actualRadiusPixels = (radiusPercent / 100.0f) * maxRadius;

                Bitmap rounded = BitmapProcessor.getRoundedCornerBitmap(original, actualRadiusPixels);
                original.recycle(); // Original is no longer needed once rounded
                return rounded;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading widget image: " + e.getMessage());
        }
        return null;
    }
}