package snoof.widget.photo;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class PhotoWidgetConfigFile extends AppCompatActivity {

    private static final String TAG = "WidgetConfigActivity";
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    // This launcher will start PhotoPicker and listen for its result
    private final ActivityResultLauncher<Intent> startPhotoPickerForResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // This callback runs when PhotoPicker finishes
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // PhotoPicker finished successfully, and it should have saved the image to internal storage.

                    // The PhotoPicker should have already called updateAppWidget, but we ensure it here too.
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
                    PhotoPickerWidgetProvider.updateAppWidget(this, appWidgetManager, mAppWidgetId);

                    // Prepare the result Intent to send back to the launcher.
                    Intent configResult = new Intent();
                    configResult.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);

                    setResult(RESULT_OK, configResult); // Tell the launcher config was successful
                    finish(); // Close the configuration activity
                } else {
                    // PhotoPicker was cancelled or failed.
                    Toast.makeText(this, "Photo selection cancelled or failed.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish(); // Close the configuration activity
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure you have a layout set here if needed, or if PhotoPicker launches automatically.
        // setContentView(R.layout.your_config_layout); // Uncomment if you have a layout

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "Invalid appWidgetId received. Finishing configuration.");
            Toast.makeText(this, "Widget configuration failed: Invalid ID.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "Configuring new widget with ID: " + mAppWidgetId);

        // *** CRITICAL CHANGE: Pass the unique appWidgetId to PhotoPicker ***
        Intent photoPickerIntent = new Intent(PhotoWidgetConfigFile.this, PhotoPicker.class);
        photoPickerIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        startPhotoPickerForResult.launch(photoPickerIntent);
    }
}