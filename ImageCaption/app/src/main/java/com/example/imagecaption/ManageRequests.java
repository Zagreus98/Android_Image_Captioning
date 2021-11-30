package com.example.imagecaption;



import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import com.android.volley.toolbox.HttpResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class ManageRequests {
    MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpg");

    public void upload(String url, File file) throws IOException {
        OkHttpClient client = new OkHttpClient();
        System.out.println(url);
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(),

                        RequestBody.create(MEDIA_TYPE_JPG, file))
                .addFormDataPart("other_field", "other_field_value")
                .build();
        System.out.println("\"File\" sent");
        Request request = new Request.Builder().url(url).post(formBody).build();
        //Response response = client.newCall(request).execute();
        client.newCall(request).execute();
    }

    public JSONObject get_json(String url, String filename) throws IOException, JSONException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response responses = null;

        try {
            responses = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String jsonData = responses.body().string();
        JSONObject jObject = new JSONObject(jsonData);
        return jObject;

    }

    public String check_server_status(String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response responses = null;
        int response_code;
        try {
            responses = client.newCall(request).execute();

        } catch (IOException e) {
            e.printStackTrace();
        }
        response_code = responses.code();
        if (response_code == 200){
            return "The server is online!";
        }
        if (response_code == 404) {
            return "The server is offline!";
        }
        else {
            return "The server is not working as intended!";
        }
    }
}
