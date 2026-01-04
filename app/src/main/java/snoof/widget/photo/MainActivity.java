package snoof.widget.photo;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * This activity is now a placeholder.
 * The app is accessed purely through the Widget Selector.
 */
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // If this is ever accidentally called, just close it.
        finish();
    }
}