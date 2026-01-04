package snoof.widget.photo; // Make sure this matches your PhotoPicker's package

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import java.io.OutputStream;
import java.io.IOException;

/**
 * A utility class for processing Bitmaps, specifically for adding curved edges
 * and outputting them to an OutputStream.
 */
public class BitmapProcessor {

    /**
     * Creates a new Bitmap with rounded corners from the given source Bitmap.
     *
     * @param bitmap The source Bitmap to be rounded.
     * @param roundPx The radius for the corners in pixels. A higher value results in more rounded corners.
     * @return A new Bitmap with rounded corners, or null if the input bitmap is null.
     */
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx) {
        if (bitmap == null) {
            return null;
        }

        // Create a new mutable bitmap with the same dimensions as the source bitmap
        // This bitmap will hold the rounded image.
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // Define the color and paint properties for drawing.
        final int color = 0xff424242; // A generic color, not visible in the final output due to Xfermode
        final Paint paint = new Paint();
        // Define the rectangle that encompasses the entire bitmap.
        final RectF rectF = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());

        // Configure the paint object.
        paint.setAntiAlias(true); // Enable anti-aliasing for smooth edges.
        canvas.drawARGB(0, 0, 0, 0); // Fill the canvas with a transparent color.
        paint.setColor(color); // Set the paint color.

        // Draw a rounded rectangle onto the canvas. This will serve as the mask.
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        // Set the Xfermode to SRC_IN. This mode uses the alpha channel of the destination
        // (the rounded rectangle) to determine where the source (the original bitmap)
        // should be drawn. Effectively, it "cuts out" the rounded shape from the bitmap.
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        // Draw the original bitmap onto the canvas using the defined Xfermode.
        // Only the parts of the bitmap that overlap with the previously drawn rounded rectangle
        // will be visible.
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return output;
    }

    /**
     * Processes a given Bitmap to have curved edges and then compresses it
     * into the provided OutputStream.
     *
     * @param originalBitmap The source Bitmap to be processed.
     * @param cornerRadius The radius for the corners in pixels.
     * @param outputStream The OutputStream where the processed Bitmap will be written.
     * @param format The compression format for the bitmap (e.g., Bitmap.CompressFormat.PNG, JPEG).
     * @param quality The compression quality (0-100). Only applies to lossy formats like JPEG.
     * @throws IOException If an I/O error occurs during writing to the stream.
     */
    public static void outputRoundedBitmap(Bitmap originalBitmap, float cornerRadius,
                                           OutputStream outputStream, Bitmap.CompressFormat format, int quality)
            throws IOException {

        if (originalBitmap == null) {
            throw new IllegalArgumentException("Original Bitmap cannot be null.");
        }
        if (outputStream == null) {
            throw new IllegalArgumentException("Output Stream cannot be null.");
        }

        // Get the bitmap with rounded corners.
        Bitmap roundedBitmap = getRoundedCornerBitmap(originalBitmap, cornerRadius);

        if (roundedBitmap != null) {
            // Compress the rounded bitmap into the output stream.
            // The compression format and quality are specified.
            roundedBitmap.compress(format, quality, outputStream);
            // Recycle the rounded bitmap to free up memory, as it's no longer needed.
            roundedBitmap.recycle();
        } else {
            // If for some reason the roundedBitmap is null (e.g., originalBitmap was null),
            // we should handle this, perhaps by throwing an exception or logging.
            // For this example, we'll throw an IOException.
            throw new IOException("Failed to create rounded bitmap.");
        }
    }
}
