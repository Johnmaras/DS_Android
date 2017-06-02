package com.example.john.testing_the_maps;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

import static com.example.john.testing_the_maps.MapsActivity.MESSAGE_IP;
import static com.example.john.testing_the_maps.MapsActivity.MESSAGE_PORT;

public class Settings extends AppCompatActivity{

    private static String master_ip;
    private static int master_port;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle extras = getIntent().getExtras();
        master_ip = extras.getString(MESSAGE_IP);
        master_port = extras.getInt(MESSAGE_PORT);

        EditText Edt_IP = (EditText)findViewById(R.id.edt_IP);
        if(master_ip != null && !master_ip.isEmpty()) Edt_IP.setHint(master_ip);

        EditText Edt_Port = (EditText)findViewById(R.id.edt_Port);
        if(master_ip != null) Edt_Port.setHint(master_port);

        //TODO add toolbar done button
        //TODO on write event check the validity of data
    }

    @Override
    public void finish(){
        Intent result_intent = new Intent();
        result_intent.putExtra(MESSAGE_IP, master_ip);
        result_intent.putExtra(MapsActivity.MESSAGE_PORT, master_port);
        setResult(RESULT_OK, result_intent);
        super.finish();
    }
}
