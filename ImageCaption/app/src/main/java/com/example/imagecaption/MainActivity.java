package com.example.imagecaption;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    Button button;
    private TextView req_str;
    private String BASE_URL = "http://10.0.2.2:5000/upload";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        req_str = (TextView) findViewById(R.id.request_get_textview);
        Button get_req = (Button) findViewById(R.id.get_req_button);

        button = (Button) findViewById(R.id.take_picture_button);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Intent i = new Intent(getApplicationContext(), CameraActivity.class);
                startActivity(i);
            }
        });

        get_req.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //requestData(BASE_URL);
                //new HTTPRequest().execute();
                new HTTPRequest().execute();
            }
        });
    }

    private void requestData(String url) {
        RequestPackage requestPackage = new RequestPackage();
        requestPackage.setMethod("GET");
        requestPackage.setUrl(url);
        //requestPackage.getParams();
        System.out.println(requestPackage.getParams());
        Downloader downloader = new Downloader(); //Instantiation of the Async task
        //that’s defined below

        downloader.execute(requestPackage);
    }

    private void uploadImage(String url, File image) throws IOException {
        ManageRequests upload_File = new ManageRequests();
        upload_File.upload(url, image);
    }

    private class Downloader extends AsyncTask<RequestPackage, String, String> {
        @Override
        protected String doInBackground(RequestPackage... params) {
            return HttpManager.getData(params[0]);
        }

        //The String that is returned in the doInBackground() method is sent to the
        // onPostExecute() method below. The String should contain JSON data.
        @Override
        protected void onPostExecute(String result) {
            try {
                //We need to convert the string in result to a JSONObject
                JSONObject jsonObject = new JSONObject(result);

                System.out.println("It works! 2");
                String price = jsonObject.getString("ask");

                //Now we can use the value in the mPriceTextView

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}


