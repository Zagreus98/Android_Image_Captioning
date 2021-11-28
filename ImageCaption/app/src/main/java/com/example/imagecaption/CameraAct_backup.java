package com.example.imagecaption;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class CameraAct_backup extends Activity
{
    private static final int CAMERA_REQUEST = 1888;
    private ImageView imageView;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private File photo_taken;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    String currentPhotoPath;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        this.imageView = (ImageView)this.findViewById(R.id.image_preview);
        Button photoButton = (Button) this.findViewById(R.id.open_camera_button);
        photoButton.setOnClickListener(new View.OnClickListener()
        {
            @SuppressLint("QueryPermissionsNeeded")
            @Override
            public void onClick(View v)
            {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                }
                else
                {
                    //Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    //startActivityForResult(cameraIntent, CAMERA_REQUEST);
                    //System.out.println("lol");
                    Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    //startActivityForResult(takePictureIntent, CAMERA_REQUEST);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

                    //dispatchTakePictureIntent();
                    System.out.println("Here");
                    System.out.println(getPackageManager());
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        // Create the File where the photo should go
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();
                        } catch (IOException ex) {
                            // Error occurred while creating the File
                            System.out.println("Something went wrong :(");
                        }
                        // Continue only if the File was successfully created
                        if (photoFile != null) {
                            System.out.println("Ajunge aici!");
                            Uri photoURI = FileProvider.getUriForFile(getApplicationContext(),
                                    "com.example.android.fileprovider",
                                    photoFile);
                            System.out.println(photoURI);
                            takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoURI);
                            //startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                            //galleryAddPic();
                        }
                    }


                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE)

        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                //Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                //startActivityForResult(cameraIntent, CAMERA_REQUEST);
                Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                //dispatchTakePictureIntent();
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }


    // For the image preview
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK)
        {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
//            //Bitmap photo = (Bitmap) data.getExtras().get("data");
            //dispatchTakePictureIntent();
            //galleryAddPic();
            System.out.println("ON ACTIVITY RESULT!");
            imageView.setImageBitmap(imageBitmap);


        }
    }

    // Invokes the intent to capture a photo
    public void dispatchTakePictureIntent() {
        System.out.println("Take picture intent.");
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                System.out.println("Something went wrong :(");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                System.out.println("Ajunge aici!");
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    // Returns the image
    private File createImageFile() throws IOException {
        // Create an image file name
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        getApplicationContext().sendBroadcast(mediaScanIntent);
    }

    private void uploadImage(String url, File image) throws IOException {
        ManageRequests upload_File = new ManageRequests();
        upload_File.upload(url, image);
    }
}