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

public class Settings extends AppCompatActivity{

    private static MenuItem okButton;

    private static String master_ip;
    private static int master_port;

    private static boolean ip_ok;
    private static boolean port_ok;

    private static boolean dataOk(){
        return ip_ok && port_ok;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ip_ok = false;
        port_ok = false;

        Bundle extras = getIntent().getExtras();
        String hint_ip = extras.getString(MESSAGE_IP);
        int hint_port = extras.getInt(MESSAGE_PORT);

        final EditText Edt_IP = (EditText)findViewById(R.id.edt_IP);
        if(hint_ip != null && !hint_ip.isEmpty()) Edt_IP.setHint(hint_ip);

        Edt_IP.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count){

                try{
                    String[] ip_octets = s.toString().trim().split("\\.");
                    if(ip_octets.length > 4){
                        ip_ok = false;
                        throw new NumberFormatException();
                    }
                    for(String octet : ip_octets){
                        int number = Integer.parseInt(octet);
                        if(number < 0 || number > 255){
                            ip_ok = false;
                            throw new NumberFormatException();
                        }else{
                            ip_ok = true;
                            ((EditText) findViewById(R.id.edt_IP)).setTextColor(Color.BLACK);
                        }
                    }
                }catch(NumberFormatException e){
                    ((EditText) findViewById(R.id.edt_IP)).setTextColor(Color.RED);
                }

                if(dataOk()) {
                    okButton.setEnabled(true);
                }else{
                    okButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        final EditText Edt_Port = (EditText)findViewById(R.id.edt_Port);
        if(hint_port != 0) Edt_Port.setHint(Integer.toString(hint_port));

        Edt_Port.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                try{
                    if(!s.equals("")){
                        int p_num = Integer.parseInt(s.toString());
                        if(p_num < 1 || p_num > 65535){
                            port_ok = false;
                            throw new NumberFormatException();
                        }else{
                            port_ok = true;
                            Edt_Port.setTextColor(Color.BLACK);
                        }
                    }else{
                        port_ok = false;
                    }
                }catch (NumberFormatException e){
                    Edt_Port.setTextColor(Color.RED);
                }

                if(dataOk()) {
                    okButton.setEnabled(true);
                }else{
                    okButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item){
                master_ip = Edt_IP.getText().toString();
                master_port = Integer.parseInt(Edt_Port.getText().toString());
                finish();
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_buttons, menu);
        okButton = menu.findItem(R.id.btn_tlb_OK).setEnabled(false);
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
