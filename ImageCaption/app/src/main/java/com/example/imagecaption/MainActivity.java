package com.example.imagecaption;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private TextView req_str;
    private String BASE_URL = "http://10.0.2.2:5000/upload";
    private Context MainContext;
    private EditText edit_url;
    private String input_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.MainContext = getApplicationContext();
//        SharedPreferences sharedPref = context.getSharedPreferences(
//                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences sharedPref = MainContext.getSharedPreferences("server_url", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        SharedPreferences sharedPrefGet = this.getSharedPreferences("server_url", MODE_PRIVATE);
        String url = sharedPrefGet.getString("url", null);
        System.out.println(url);
        if (url != "Server URL") {
            edit_url = (EditText) this.findViewById(R.id.edit_server_url_input);
            //url = "http://10.0.2.2:5000";
            edit_url.setText(url);
        }

        // Button that goes to the application
        Button Application_button = (Button) findViewById(R.id.to_application_button);
        Button Application_button_auto = (Button) findViewById(R.id.to_application_button2);
        Application_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                //editor.putString("url", getString(R.string.server_url));
                editor.putString("url", String.valueOf(edit_url.getText()));
                editor.apply();
                Intent i = new Intent(getApplicationContext(), CameraActivity.class);
                startActivity(i);
            }
        });

        Application_button_auto.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                //editor.putString("url", getString(R.string.server_url));
                editor.putString("url", "androidcaption.ddns.net:85");
                editor.apply();
                Intent i = new Intent(getApplicationContext(), CameraActivity.class);
                startActivity(i);
            }
        });

    }

}


