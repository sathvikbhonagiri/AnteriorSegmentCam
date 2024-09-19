package com.example.watermark;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentUris;
import android.content.Intent;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static final String CAMERA_FRONT = "1";
    public static final String CAMERA_BACK = "0";

    private String cameraId = CAMERA_BACK;
    private boolean isFlashSupported;
    private boolean isTorchOn;

    private TextureView textureView;
    private String fname;
    private int i=0;

    private CameraDevice cameraDevice;
    private ImageButton captureButton;
    private ImageButton flashButton;
    private ImageReader imageReader;
    private CameraManager cameraManager;
    private RadioGroup rgroup;
    private RadioButton tox;
    private RadioButton fox;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = findViewById(R.id.textureView);
        captureButton = findViewById(R.id.capture);

        rgroup=findViewById(R.id.rgroup);
        tox=findViewById(R.id.tox);
        fox=findViewById(R.id.fox);
        flashButton = findViewById(R.id.flashlight_button);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            cameraId = getCameraId();
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Failed to get camera ID", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            isFlashSupported = available != null && available;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        setupFlashButton();


        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImage();
            }
        });

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                // Handle surface size change if needed
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                closeCamera();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                // Do nothing here
            }
        });

        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchFlash();
            }
        });

        rgroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                RadioButton checkedRadioButton = (RadioButton) findViewById(checkedId);
                // Get the text of the checked RadioButton
                int seid = checkedRadioButton.getId();
                if (seid==R.id.tox)
                {
                    i=0;
                    startPreview();
                }
                else
                {
                    i=1;
                    startPreview();

                }
            }
        });


    }
    private void switchFlash() {
        try {
            if (cameraId.equals(CAMERA_BACK)) {
                if (isFlashSupported) {
                    if (isTorchOn) {
                        previewBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                        captureSession.setRepeatingRequest(previewBuilder.build(), null, null);
                        isTorchOn = false;
                    } else {
                        previewBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                        captureSession.setRepeatingRequest(previewBuilder.build(), null, null);
                        isTorchOn = true;
                    }

                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void setupFlashButton() {
        if (cameraId.equals(CAMERA_BACK) && isFlashSupported) {
            flashButton.setVisibility(View.VISIBLE);

            if (isTorchOn) {
                //  flashButton.setImageResource(R.drawable.ic_flash_off);
            } else {
                //flashButton.setImageResource(R.drawable.ic_flash_on);
            }

        } else {
            flashButton.setVisibility(View.GONE);
        }
    }

    private void startPreview() {
        SurfaceTexture texture = textureView.getSurfaceTexture();
        assert texture != null;

        int textureViewWidth = textureView.getWidth();
        int textureViewHeight = textureView.getHeight();
        int previewSize = Math.max(textureViewWidth, textureViewHeight);

        texture.setDefaultBufferSize(previewSize, previewSize);
        Surface surface = new Surface(texture);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String ts = sdf.format(new Date());
        fname = "captured_image_" + ts + ".jpg";

        // Setting ImageReader to the maximum resolution supported by the camera
        CameraCharacteristics characteristics = null;
        try {
            characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Rect activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            imageReader = ImageReader.newInstance(activeArraySize.width(), activeArraySize.height(), ImageFormat.JPEG, 1);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        try {
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(surface);
            if(i==0) {

                setZoom(previewBuilder);
            }
            else
                setZoomfour(previewBuilder);


            if (isFlashSupported && isTorchOn) {
                previewBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            } else {
                previewBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            }

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    captureSession = session;
                    try {
                        captureSession.setRepeatingRequest(previewBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e("Camera", "Failed to configure camera preview.");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied. Cannot open camera.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 1);
            return;
        }

        try {
            if (cameraId == null) {
                Log.e("Camera", "No suitable camera found.");
                return;
            }

            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    startPreview();
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();

        }
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    private void captureImage() {
        try {
            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());

            if(i==0) {
                setZoom(captureBuilder);
            }
            else
                setZoomfour(captureBuilder);


            if (isFlashSupported && isTorchOn) {
                captureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            } else {
                captureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            }

            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    FileOutputStream output = null;
                    try {
                        image = reader.acquireNextImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                        // Rotate the bitmap if needed
                        bitmap = rotateBitmap(bitmap, 90);
                        Intent intent = getIntent();
                        String Result = intent.getStringExtra("Result");
                        // Add watermark to the bitmap
                        Bitmap TextwatermarkedBitmap = addWatermark(bitmap, Result);
                        int watermarkResId = R.drawable.watermarkimg;
                        Bitmap result = addImageWatermark(MainActivity.this, TextwatermarkedBitmap, watermarkResId);
                        // Save the final bitmap to the gallery with high quality
                        saveBitmapToGallery(result, fname);

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
            }, null);

            captureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(MainActivity.this, "image saved to gallery", Toast.LENGTH_SHORT).show();
                    startPreview();
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private Bitmap addWatermark(Bitmap src, String watermark) {
        Bitmap result = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(src, new Matrix(), null);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(50);
        textPaint.setAntiAlias(true);
        textPaint.setUnderlineText(false);

        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setStyle(Paint.Style.FILL);

        String[] lines = watermark.split("\n");

        Rect textBounds = new Rect();
        int maxWidth = 0;
        int totalTextHeight = 0;
        int padding = 10;
        for (String line : lines) {
            textPaint.getTextBounds(line, 0, line.length(), textBounds);
            maxWidth = Math.max(maxWidth, textBounds.width());
            totalTextHeight += textBounds.height() + padding;
        }

        int x = src.getWidth() - maxWidth - 2 * padding;
        int y = padding;
        if(!watermark.equals("")) {
            canvas.drawRect(
                    x - padding,
                    y - padding,
                    x + maxWidth + padding + 100,
                    y + totalTextHeight - padding / 2,
                    backgroundPaint
            );
        }
        int currentY = y;
        for (String line : lines) {
            textPaint.getTextBounds(line, 0, line.length(), textBounds);
            canvas.drawText(line, x, currentY + textBounds.height(), textPaint);
            currentY += textBounds.height() + padding;
        }

        return result;
    }
    private Bitmap addImageWatermark(Context context, Bitmap src, int watermarkResId) {

        Bitmap watermarkImage = BitmapFactory.decodeResource(context.getResources(), watermarkResId);
        Bitmap result = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(src, new Matrix(), null);

        int watermarkImageWidth = src.getWidth() / 3;
        int watermarkImageHeight = (watermarkImage.getHeight() * watermarkImageWidth) / watermarkImage.getWidth();
        Bitmap scaledWatermarkImage = Bitmap.createScaledBitmap(watermarkImage, watermarkImageWidth, watermarkImageHeight, false);

        int padding = 13;
        int imageX = padding+80;
        int imageY = src.getHeight()-scaledWatermarkImage.getHeight() - padding+15;

        canvas.drawBitmap(scaledWatermarkImage, imageX, imageY, null);


        return result;
    }



    private void saveBitmapToGallery(Bitmap bitmap, String fileName) throws IOException {
        String albumName = "AnteriCAM"; // Define your album name here

        // Create a new ContentValues object
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        // Check if the album exists
        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        String selection = MediaStore.Images.Media.RELATIVE_PATH + "=?";
        String[] selectionArgs = new String[]{Environment.DIRECTORY_PICTURES + "/" + albumName};

        Cursor cursor = getContentResolver().query(collection, new String[]{MediaStore.Images.Media._ID}, selection, selectionArgs, null);

        if (cursor != null && cursor.getCount() > 0) {
            // Album exists
            cursor.moveToFirst();
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            Uri existingAlbumUri = ContentUris.withAppendedId(collection, id);
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + albumName);
            getContentResolver().insert(existingAlbumUri, values);
        } else {
            // Album does not exist, create it
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + albumName);
            getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        }

        if (cursor != null) {
            cursor.close();
        }

        // Insert the image
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            try (FileOutputStream out = (FileOutputStream) getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            }
        }
    }


    private String getCameraId() throws CameraAccessException {
        for (String cameraId : cameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId;
            }
        }
        return null;
    }

    private void setZoom(CaptureRequest.Builder builder) {
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
            Rect activeRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

            float maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
            if (maxZoom > 1) {
                Rect cropRegion = new Rect(activeRect);
                int cropWidth = (int) (activeRect.width() / 2);
                int cropHeight = (int) (activeRect.height() / 2);
                cropRegion.left = (activeRect.width() - cropWidth) / 2;
                cropRegion.top = (activeRect.height() - cropHeight) / 2;
                cropRegion.right = cropRegion.left + cropWidth;
                cropRegion.bottom = cropRegion.top + cropHeight;

                builder.set(CaptureRequest.SCALER_CROP_REGION, cropRegion);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setZoomfour(CaptureRequest.Builder requestBuilder) {
        CameraCharacteristics characteristics = null;
        try {
            characteristics = cameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Rect rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        float cropFactor = 4f; // This means no zoom
        int cropW = (int) (rect.width() / cropFactor);
        int cropH = (int) (rect.height() / cropFactor);
        int cropX = (rect.width() - cropW) / 2;
        int cropY = (rect.height() - cropH) / 2;
        Rect cropRect = new Rect(cropX, cropY, cropX + cropW, cropY + cropH);
        requestBuilder.set(CaptureRequest.SCALER_CROP_REGION, cropRect);
    }
    @Override
    protected void onResume() {
        super.onResume();
        openCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
    }

}
