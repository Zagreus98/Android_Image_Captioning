package com.example.imagecaption;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.loader.content.CursorLoader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;

public class CameraActivity extends Activity {
    private static final int CAMERA_REQUEST = 1888;
    private ImageView imageView;
    private TextView caption_textview;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private File photo_taken;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    String currentPhotoPath;
    private ContentValues values;
    private Uri imageUri;
    private Uri photoUri;
    private static final int PICTURE_RESULT = 122 ;
    private Bitmap thumbnail;
    private File gallery_image;
    private FileInputStream gallery_image_stream;
    String imageurl;
    String image_name;
    String caption_text;
    String tts_url;
    MediaPlayer mediaPlayer;
    InputStream inputStream;
    InputStreamToFile inputStreamToFile;
    BufferedInputStream bufferedInputStream;
    public static final int PICK_IMAGE = 2;
    public static final int DEFAULT_BUFFER_SIZE = 8192;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        this.imageView = (ImageView) this.findViewById(R.id.image_preview);
        this.caption_textview = (TextView) this.findViewById(R.id.caption_textview);
        Button photoButton = (Button) this.findViewById(R.id.open_camera_button);
        Button captionButton = (Button) this.findViewById(R.id.replay_caption_button);
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

//                System.out.println("Values");
//                System.out.println(values);

                imageUri = getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
//                System.out.println(imageUri);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

//                System.out.println("URI");
//                System.out.println(imageUri);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, PICTURE_RESULT);

                Runnable r = () -> {
                    captionButton.setVisibility(View.VISIBLE);
                    playAudio(tts_url);
                };

                Handler h = new Handler();
                h.postDelayed(r, 1000); // <-- the "1000" is the delay time in miliseconds.
            }
        });

        captionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println(tts_url);
                playAudio(tts_url);
            }
        });

        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
                captionButton.setVisibility(View.VISIBLE);
            }
        });



    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICTURE_RESULT) {

            try {
                thumbnail = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imageView.setImageBitmap(thumbnail);
                imageurl = getRealPathFromURI(imageUri);
                //@SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

                // Load file from path
                File image_file = new File(imageurl);

//                System.out.println("Path");
//                System.out.println(imageurl);
//                System.out.println(imageUri);

                OutputStream os = new BufferedOutputStream(new FileOutputStream(image_file));
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, os);


                uploadImage("http://10.0.2.2:5000/upload", image_file);
                JSONObject json_file = getCaption_tts(String.format("http://10.0.2.2:5000/check/%s", image_name), image_name);
                caption_text = json_file.getString("caption");
                tts_url = json_file.getString("audio_url");


                caption_textview.setText(caption_text);
                //playAudio(tts_url);

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Flask server is not working!");
            }
        }

        if (requestCode == PICK_IMAGE) {
            if (data == null) {
                //Display an error
                System.out.println("Null");
                return;
            }
            Uri selectedImage = data.getData();
            System.out.println(selectedImage);
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


            // Some useful prints
            //System.out.println(returnCursor.getString(pathIndex));
            System.out.println("Some file info");
            //System.out.println(returnCursor.getString(pathIndex));
            System.out.println(returnCursor.getString(nameIndex));
            System.out.println(returnCursor.getLong(sizeIndex));
            imageView.setImageURI(returnUri);


            // IMAGE FROM BITMAP
            try {
                //Load bitmap from Uri and create empty file
                Bitmap gallery_bitmap = getBitmapFromUri(returnUri, getApplicationContext());
                gallery_image = new File(getApplicationContext().getCacheDir(), returnCursor.getString(nameIndex));
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
                uploadImage("http://10.0.2.2:5000/upload", gallery_image);
                //System.out.println(imageUri);
                JSONObject json_file = getCaption_tts(String.format("http://10.0.2.2:5000/check/%s", returnCursor.getString(nameIndex)), returnCursor.getString(nameIndex));

                caption_text = json_file.getString("caption");
                tts_url = json_file.getString("audio_url");
                caption_textview.setText(caption_text);

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }


           returnCursor.close();


        }
    }


    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        //System.out.println("Real Path.");
        //System.out.println(cursor.getString(column_index));
        String r_path = cursor.getString(column_index);
        cursor.close();
        return r_path;
    }

    private String getRealPathFromURI2(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(getApplicationContext(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    private Bitmap getBitmapFromUri(Uri uri, Context context) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                context.getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    private void uploadImage(String url, File image) throws IOException {
        ManageRequests upload_File = new ManageRequests();
        upload_File.upload(url, image);
    }

    private JSONObject getCaption_tts(String url, String image_name) throws IOException, JSONException {
        ManageRequests json_file = new ManageRequests();
        //json_file.get_json(url, image_name);
        return json_file.get_json(url, image_name);
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
