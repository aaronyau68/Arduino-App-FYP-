package com.example.arduinousb;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by Aaron on 7/3/2018.
 */

public class MainActivity extends Activity{
    Button btnGame, btnRecycle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnGame = (Button)findViewById(R.id.btnGame);
        btnRecycle = (Button)findViewById(R.id.btnRecycle);
    }

    public void startGame(View view){
        Intent myIntent = new Intent(MainActivity.this, CommunicateActivity.class);
      //  myIntent.putExtra("key", value); //Optional parameters
        MainActivity.this.startActivity(myIntent);
    }

    public void startRecycle(View view){
        Intent myIntent = new Intent(MainActivity.this, RecognizingActivity.class);
        //  myIntent.putExtra("key", value); //Optional parameters
        MainActivity.this.startActivity(myIntent);
    }
}
