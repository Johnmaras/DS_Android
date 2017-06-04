package com.example.john.testing_the_maps;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import static com.example.john.testing_the_maps.MapsActivity.MESSAGE_IP;
import static com.example.john.testing_the_maps.MapsActivity.MESSAGE_PORT;

//FIXME intent results don't return

public class Settings extends AppCompatActivity{

    private static final int REQUEST_CODE = 2610;

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

        final EditText Edt_IP = (EditText)findViewById(R.id.edt_IP);
        if(master_ip != null && !master_ip.isEmpty()) Edt_IP.setHint(master_ip);

        Edt_IP.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    String[] ip_octets = s.toString().trim().split("\\.");
                    if(ip_octets.length > 4) throw new NumberFormatException();
                    for (String octet : ip_octets) {

                        int number = Integer.parseInt(octet);
                        if (number < 1 || number > 255) {
                            throw new NumberFormatException();
                        } else {
                            ((EditText) findViewById(R.id.edt_IP)).setTextColor(Color.BLACK);
                        }
                    }
                }catch(NumberFormatException e){
                    ((EditText) findViewById(R.id.edt_IP)).setTextColor(Color.RED);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        final EditText Edt_Port = (EditText)findViewById(R.id.edt_Port);
        if(master_ip != null) Edt_Port.setHint(master_port);

        Edt_Port.addTextChangedListener(new TextWatcher() {
            String oldText;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                oldText = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try{
                    if(!s.toString().equals(oldText)){
                        if(!s.equals("")){
                            int p_num = Integer.parseInt(s.toString());
                            if(p_num < 1 || p_num > 65535){
                                throw new NumberFormatException();
                            }else{
                                Edt_Port.setTextColor(Color.BLACK);
                            }
                        }
                    }
                }catch (NumberFormatException e){
                    Edt_Port.setTextColor(Color.RED);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                finishActivity(REQUEST_CODE);
                return true;
            }
        });
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_buttons, menu);
        return true;
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
