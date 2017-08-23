package com.sk.moai;
import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import android.os.Build;
import android.annotation.TargetApi;
import java.util.HashMap;
import java.util.Locale;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY = "AIzaSyBxx49_cm3Q0PgJEI5rWH69AUuMRvLmaL4";
    public static final String FILE_NAME = "temp.jpg";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    public static  String FEATURE_TYPE = "LABEL_DETECTION";

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;

    private TextView mImageDetails;
    private ImageView mMainImage;
    private Button btnDetection ;
    private Button btnText ;
    private Button btnLandmark ;

    // TTS 설정
    private TextToSpeech tts;
    private Locale mLocale = Locale.US;
    private float mPitch = (float) 0;
    private float mRate = (float) 0;
    private int mQueue = TextToSpeech.QUEUE_ADD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        final CameraSurfaceView cameraView = new CameraSurfaceView(getApplicationContext());

        FrameLayout previewFrame = (FrameLayout) findViewById(R.id.frameLayout);
        mImageDetails = (TextView) findViewById(R.id.imageResult);
        previewFrame.addView(cameraView);

        tts = new TextToSpeech(getBaseContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS) {
                } else{
                    // todo: fail 시 처리
                }
            }
        });

        btnDetection = (Button)findViewById(R.id.btnDetection);
        btnText = (Button)findViewById(R.id.btnText);
        btnLandmark = (Button)findViewById(R.id.btnLandmark);

        btnDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FEATURE_TYPE = "LABEL_DETECTION";
            }
        });
        btnText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FEATURE_TYPE = "TEXT_DETECTION";
            }
        });
        btnLandmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FEATURE_TYPE = "LANDMARK_DETECTION";
            }
        });

        previewFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Camera mCamera = cameraView.getCamera();
                mCamera.autoFocus (new Camera.AutoFocusCallback() {
                    public void onAutoFocus(boolean success, Camera camera) {
                        if(success){
                            cameraView.capture(new Camera.PictureCallback(){
                                public void onPictureTaken(byte[] data, Camera camera){
                                    try{
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                        Log.d("Bitmap : ",  bitmap.toString() + " / " + data.length );
                                        mImageDetails.setText("");
                                        uploadImage(bitmap);
                                        camera.startPreview();
                                    }catch (Exception e){
                                        Log.e("SmapleCapture","Failed to insert Image",e);
                                    }
                                }
                            });
                        }
                        else{
                            //Toast.makeText(getApplicationContext(),"Auto Focus Failed",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
/*
    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }

    public void startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
            case GALLERY_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, GALLERY_PERMISSIONS_REQUEST, grantResults)) {
                    startGalleryChooser();
                }
                break;
        }
    }
*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Bitmap bitmap
    public void uploadImage(Bitmap bitmap) {
        if (bitmap != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap downBitmap = scaleBitmapDown( bitmap,  1200);
                callCloudVision(downBitmap);
                //mMainImage.setImageBitmap(bitmap);

            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

    private void callCloudVision(final Bitmap bitmap) throws IOException {
        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer requestInitializer =
                            new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                                /**
                                 * We override this so we can inject important identifying fields into the HTTP
                                 * headers. This enables use of a restricted cloud platform API key.
                                 */
                                @Override
                                protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                                        throws IOException {
                                    super.initializeVisionRequest(visionRequest);

                                    String packageName = getPackageName();
                                    visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                                    String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                                    visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                                }
                            };

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);

                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                        // Add the image
                        Image base64EncodedImage = new Image();
                        // Convert the bitmap to a JPEG
                        // Just in case it's a format that Android understands but Cloud Vision
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();

                        // Base64 encode the JPEG
                        base64EncodedImage.encodeContent(imageBytes);
                        annotateImageRequest.setImage(base64EncodedImage);

                        // add the features we want
                        annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                            Feature labelDetection = new Feature();
                            labelDetection.setType(FEATURE_TYPE);
                            //labelDetection.setType("TEXT_DETECTION");
                            labelDetection.setMaxResults(10);
                            add(labelDetection);
                        }});

                        // Add the list of one thing to the request
                        add(annotateImageRequest);
                    }});

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    Log.d(TAG, "created Cloud Vision request object, sending request");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return convertResponseToString(response);

                } catch (GoogleJsonResponseException e) {
                    Log.d(TAG, "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    Log.d(TAG, "failed to make API request because of other IOException " +
                            e.getMessage());
                }
                return "Cloud Vision API request failed. Check logs for details.";
            }

            protected void onPostExecute(String result) {
                mImageDetails.setText(result);
                String text = mImageDetails.getText().toString();
                if(text.length() > 0) {
                    Log.d("@@@@@@@ text",text);
                    setLanguage(mLocale);
                    setPitch(mPitch);
                    setSpeechRate(mRate);
                    speak(text, 0);
                }
            }
        }.execute();
    }

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        String result = "" ;
        String message = "" ;
        List<EntityAnnotation> labels = null ;

        if( FEATURE_TYPE.equals("LABEL_DETECTION")  ) {
            labels = response.getResponses().get(0).getLabelAnnotations();
        }else if(  FEATURE_TYPE.equals("TEXT_DETECTION")  ){
            labels = response.getResponses().get(0).getTextAnnotations();
        }else if( FEATURE_TYPE.equals("LANDMARK_DETECTION") ){
            labels = response.getResponses().get(0).getLandmarkAnnotations();
        }

        if (labels != null) {
            result = labels.get(0).getDescription() ;
            for (EntityAnnotation label : labels) {
                message += String.format(Locale.US, "%.3f: %s : %s", label.getScore(), label.getDescription() , label.getLocations());
                message += "\n";
            }
            Log.d("#### Response : ", FEATURE_TYPE + " / " + message );
        } else {
            Log.d("#### Response : ", FEATURE_TYPE + " / " +  "I don't know..." );
            result += "I don't know...";
        }
        return result;
    }

    /******************************************************
     * TTS 관련 함수
     ******************************************************/
    @Override
    protected void onDestroy() {
        if (tts != null) {
            if(tts.isSpeaking()) {
                tts.stop();
            }
            tts.shutdown();
        }
        super.onDestroy();
    }
    /** 언어 선택 */
    public void setLanguage(Locale locale){
        if(tts!=null)
            tts.setLanguage(locale);
    }

    public void setPitch(float value){
        if(tts!=null)
            tts.setPitch(value);
    }

    /** 속도 선택 */
    public void setSpeechRate(float value){
        if(tts!=null)
            tts.setSpeechRate(value);
    }
    /** 재생 */
    public void speak(String text, int resId){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(tts != null){
                tts.speak(text, mQueue, null, ""+resId);
            }
        }else{
            HashMap<String, String> map = new HashMap<>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, ""+resId);
            if(tts != null)
                tts.speak(text, mQueue, map);
        }
    }

}
