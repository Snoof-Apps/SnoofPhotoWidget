package snoof.widget.photo;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PhotoPicker extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "PhotoPicker";
    private static final String PREFS_NAME = "PhotoWidgetPrefs";
    private static final String PREF_RADIUS_KEY = "radius_percent_";

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private Button btn_pickmedia;
    private ImageView view_pickmedia;
    private SeekBar radiusSeekBar;
    private TextView radiusLabelText;

    private Bitmap originalImageBitmap;
    private Bitmap imgBitmap;
    private File imgDir;

    private int currentRadiusPercent = 50;

    ActivityResultLauncher<PickVisualMediaRequest> pickVisualMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        if (originalImageBitmap != null && !originalImageBitmap.isRecycled()) {
                            originalImageBitmap.recycle();
                        }
                        originalImageBitmap = BitmapFactory.decodeStream(inputStream);
                        if (inputStream != null) inputStream.close();

                        if (originalImageBitmap != null) {
                            refreshPreview();
                            saveRadiusPercentage(currentRadiusPercent);
                            saveImageToInternalStorage(mAppWidgetId);

                            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
                            PhotoPickerWidgetProvider.updateAppWidget(this, appWidgetManager, mAppWidgetId);

                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(this, "Failed to decode image.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error: " + e.getMessage());
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.photo_picker_layout);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.photopicker), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAppWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // Standard storage permission request
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2006);

        imgDir = new ContextWrapper(getApplicationContext()).getDir("imgDir", MODE_PRIVATE);
        btn_pickmedia = findViewById(R.id.pickmediabutton);
        view_pickmedia = findViewById(R.id.pickmediaview);
        radiusSeekBar = findViewById(R.id.radius_seekbar);
        radiusLabelText = findViewById(R.id.radius_label_text);

        loadRadiusPercentage();
        radiusSeekBar.setProgress(currentRadiusPercent);
        updateRadiusText(currentRadiusPercent);

        radiusSeekBar.setOnSeekBarChangeListener(this);
        loadImageFromInternalStorage(mAppWidgetId);

        btn_pickmedia.setOnClickListener(v -> {
            pickVisualMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
    }

    private float calculateDynamicRadius(Bitmap bitmap, int percent) {
        if (bitmap == null) return 0f;
        // Full circle radius = half of the shortest side
        float maxRadius = Math.min(bitmap.getWidth(), bitmap.getHeight()) / 2.0f;
        return (percent / 100.0f) * maxRadius;
    }

    private void refreshPreview() {
        if (originalImageBitmap != null && !originalImageBitmap.isRecycled()) {
            if (imgBitmap != null && !imgBitmap.isRecycled()) {
                imgBitmap.recycle();
            }
            float radius = calculateDynamicRadius(originalImageBitmap, currentRadiusPercent);
            imgBitmap = BitmapProcessor.getRoundedCornerBitmap(originalImageBitmap, radius);
            view_pickmedia.setImageBitmap(imgBitmap);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            currentRadiusPercent = progress;
            updateRadiusText(progress);
            refreshPreview();
        }
    }

    private void updateRadiusText(int percentage) {
        radiusLabelText.setText("Corner Radius: " + percentage + "%");
    }

    private void saveRadiusPercentage(int percentage) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putInt(PREF_RADIUS_KEY + mAppWidgetId, percentage).apply();
    }

    private void loadRadiusPercentage() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentRadiusPercent = prefs.getInt(PREF_RADIUS_KEY + mAppWidgetId, 50);
    }

    private void saveImageToInternalStorage(int appWidgetId) {
        if (originalImageBitmap == null || originalImageBitmap.isRecycled()) return;

        File imgSavePath = new File(imgDir, "pic_" + appWidgetId + ".png");
        Bitmap roundedToSave = null;

        try (FileOutputStream out = new FileOutputStream(imgSavePath)) {
            float radius = calculateDynamicRadius(originalImageBitmap, currentRadiusPercent);
            roundedToSave = BitmapProcessor.getRoundedCornerBitmap(originalImageBitmap, radius);
            if (roundedToSave != null) {
                roundedToSave.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
        } catch (IOException e) {
            Log.e(TAG, "Save Error: " + e.getMessage());
        } finally {
            if (roundedToSave != null && !roundedToSave.isRecycled()) {
                roundedToSave.recycle();
            }
        }
    }

    private void loadImageFromInternalStorage(int appWidgetId) {
        File imgFile = new File(imgDir, "pic_" + appWidgetId + ".png");
        if (!imgFile.exists()) {
            view_pickmedia.setImageResource(R.drawable.pfptest);
            return;
        }

        try (FileInputStream fis = new FileInputStream(imgFile)) {
            if (originalImageBitmap != null && !originalImageBitmap.isRecycled()) {
                originalImageBitmap.recycle();
            }
            originalImageBitmap = BitmapFactory.decodeStream(fis);
            refreshPreview();
        } catch (Exception e) {
            Log.e(TAG, "Load Error: " + e.getMessage());
            view_pickmedia.setImageResource(R.drawable.pfptest);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imgBitmap != null && !imgBitmap.isRecycled()) imgBitmap.recycle();
        if (originalImageBitmap != null && !originalImageBitmap.isRecycled()) originalImageBitmap.recycle();
    }

    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
}