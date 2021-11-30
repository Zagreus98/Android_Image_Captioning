package com.example.imagecaption;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public class CameraActivity extends Activity {
    private ImageView imageView;
    private TextView caption_textview;
    private ContentValues values;
    private Uri imageUri;
    private static final int PICTURE_RESULT = 122 ;
    private Bitmap thumbnail;
    private File gallery_image;
    private TextView server_status;
    private ImageView server_status_code;
    static String url = "http://";
    String imageurl;
    String image_name;
    String caption_text;
    String tts_url;
    MediaPlayer mediaPlayer;
    InputStream inputStream;
    BufferedInputStream bufferedInputStream;
    public static final int PICK_IMAGE = 2;
    private static final int UPLOAD = 4 ;
    private static final int CAPTION = 5 ;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Get the URL inserted in MainActivity
        SharedPreferences sharedPrefGet = this.getSharedPreferences("server_url", MODE_PRIVATE);
        url = url + sharedPrefGet.getString("url", null);


        check_status(url);

        this.imageView = (ImageView) this.findViewById(R.id.image_preview);
        this.caption_textview = (TextView) this.findViewById(R.id.caption_textview);
        Button photoButton = (Button) this.findViewById(R.id.open_camera_button);
        Button audioButton = (Button) this.findViewById(R.id.replay_caption_button);
        Button captionButton = (Button) this.findViewById(R.id.get_caption_tts_button);
        Button galleryButton = (Button) this.findViewById(R.id.select_from_gallery_button);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                //System.out.println("Values");

                values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, timeStamp);

                // Works as intended!
                image_name = timeStamp;

                values.put(MediaStore.Images.Media.DISPLAY_NAME, timeStamp);
                values.put(MediaStore.Images.Media.DESCRIPTION, "picture");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

                // Add the date meta data to ensure the image is added at the front of the gallery
                values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

                // Create the URI for the image that will be taken
                imageUri = getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                // Where to store and launch camera app
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, PICTURE_RESULT);

                // Make Caption Button visible
                audioButton.setVisibility(View.VISIBLE);

            }
        });

        audioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Plays the TTS
                System.out.println(tts_url);
                playAudio(tts_url);
            }
        });

        captionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Plays the TTS
                //uploadAction();
                Toast.makeText(CameraActivity.this, "Not implemented yet!", Toast.LENGTH_SHORT).show();
            }
        });

        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
                audioButton.setVisibility(View.VISIBLE);
            }
        });

    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Case for taking a picture with the camera
        if (requestCode == PICTURE_RESULT) {

            try {
                // Thumbnail that will be shown in the layout
                thumbnail = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imageView.setImageBitmap(thumbnail);
                // Get the URL from URI
                imageurl = getRealPathFromURI(imageUri);

                // Load file from path
                File image_file = new File(imageurl);

                // Prepare image to be uploaded
                OutputStream os = new BufferedOutputStream(new FileOutputStream(image_file));
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, os);

                // Upload image
                uploadAction(url, image_name, image_file, UPLOAD);

                // Pause thread or introduce sleep time
                caption_textview.setText(caption_text);
                //playAudio(tts_url);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Flask server is not working!", Toast.LENGTH_SHORT).show();
                System.out.println("Flask server is not working!");
            }
        }

        // Case for picking an image from the gallery
        if (requestCode == PICK_IMAGE) {
            if (data == null) {
                //Display an error
                System.out.println("Null");
                return;
            }
            // Grabs the URI
            Uri selectedImage = data.getData();
//            System.out.println(selectedImage);
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();


            // THIS WORKS - PREVIEW IMAGE
            // -------------------------
            try {
                inputStream = getApplicationContext().getContentResolver().openInputStream(data.getData());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            bufferedInputStream = new BufferedInputStream(inputStream);
            thumbnail = BitmapFactory.decodeStream(bufferedInputStream);
            //imageView.setImageBitmap(thumbnail);

            cursor.close();
            cursor = null;
            // ---------------------------

            // GET FILE INFO
            Uri returnUri = data.getData();
                String mimeType = getContentResolver().getType(returnUri);
                System.out.println(mimeType);
            Cursor returnCursor =
                    getContentResolver().query(returnUri, null, null, null, null);

            returnCursor.moveToFirst();
            // TEMPORARY - mainly for print
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);

            // Preview the image
            imageView.setImageURI(returnUri);
            String gallery_image_filename = returnCursor.getString(nameIndex);
            returnCursor.close();

            // To make sure that the cursor clears itself
            returnCursor = null;

            // IMAGE FROM BITMAP
            try {
                //Load bitmap from Uri and create empty file
                Bitmap gallery_bitmap = getBitmapFromUri(returnUri, getApplicationContext());
                gallery_image = new File(getApplicationContext().getCacheDir(), gallery_image_filename);
                gallery_image.createNewFile();

                //Convert bitmap to byte array
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                gallery_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                byte[] bitmapdata = bos.toByteArray();

                //Write the bytes in file
                FileOutputStream fos = new FileOutputStream(gallery_image);
                fos.write(bitmapdata);
                fos.flush();
                fos.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Image file");

            try {

                uploadAction(url, gallery_image_filename, gallery_image, UPLOAD);
                caption_textview.setText(caption_text);

            } catch (IOException | JSONException e) {
                e.printStackTrace();
                Toast.makeText(this, "Flask server is not working!", Toast.LENGTH_SHORT).show();
                System.out.println("Flask server is not working!");
            }

        }
    }


    public String getRealPathFromURI(Uri contentUri) {
        // Open URI with cursor
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        // Retrieve absolute path
        String r_path = cursor.getString(column_index);
        cursor.close();
        cursor = null;
        return r_path;
    }


    private Bitmap getBitmapFromUri(Uri uri, Context context) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                context.getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    // Initialize the function from ManageRequests class
    private void uploadImage(String url, File image) throws IOException {
        ManageRequests upload_File = new ManageRequests();
        upload_File.upload(url, image);
    }

    // Function that uploads the file and returns the caption and TTS url
    private void uploadAction(String url, String filename, File image, int Action_code) throws IOException, JSONException {
        if (Action_code == UPLOAD) {
            // Uploads the image to the server
            uploadImage(url + "/upload", image);

            // Retrieves the JSON object
            JSONObject json_file = getCaption_tts(String.format(url + "/check/%s", filename), filename);

            // Changes the variables with the retrieved strings
            caption_text = json_file.getString("caption");
            tts_url = json_file.getString("audio_url");
        }
        if (Action_code == CAPTION) {
            // Retrieves the JSON object
            JSONObject json_file = getCaption_tts(String.format(url + "/check/%s", filename), filename);

            // Changes the variables with the retrieved strings
            caption_text = json_file.getString("caption");
            tts_url = json_file.getString("audio_url");
        }
    }

    // Initialize the function from ManageRequests class that returns the JSON object with the caption and TTS
    private JSONObject getCaption_tts(String url, String image_name) throws IOException, JSONException {
        ManageRequests json_file = new ManageRequests();
        return json_file.get_json(url, image_name);
    }

    // Checks Server Status
    private void check_status(String url) {
        ManageRequests url_status = new ManageRequests();
        String status = url_status.check_server_status(url);

        if (status.equals("The server is online!")){
            server_status.setText(status);
            int id = getResources().getIdentifier("com.example.imagecaption:drawable/" + "green_dot", null, null);
            server_status_code.setImageResource(id);

        }
        else {
            server_status.setText(status);
            int id = getResources().getIdentifier("com.example.imagecaption:drawable/" + "red_dot", null, null);
            server_status_code.setImageResource(id);
        }
    }

    private void playAudio(String audioUrl) {

        String audioUrl2 = "https://polly.streamlabs.com/v1/speech?OutputFormat=mp3&Text=THIS%20IS%20NEW%21&VoiceId=Brian&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIHKNQTJ7BGLEFVZA%2F20211127%2Fus-west-2%2Fpolly%2Faws4_request&X-Amz-Date=20211127T142807Z&X-Amz-SignedHeaders=host&X-Amz-Expires=900&X-Amz-Signature=46ea705eef5dacbde430198de1af98cfde8c21a37f122e0c7c59c17c62914e30";

        // initializing media player
        mediaPlayer = new MediaPlayer();

        // below line is use to set the audio
        // stream type for our media player.
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        // below line is use to set our
        // url to our media player.
        try {
            mediaPlayer.setDataSource(audioUrl);
            // below line is use to prepare
            // and start our media player.
            mediaPlayer.prepare();
            mediaPlayer.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
        // below line is use to display a toast message.
        Toast.makeText(this, "Text-2-Speech started", Toast.LENGTH_SHORT).show();
    }


}
