package com.jilexandr.trackyourbus;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

public class BaseActivity extends AppCompatActivity {

   /* protected TextView mTextMessage;

    protected void setup() {
        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                System.out.println(item.getItemId());
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        setContentView(R.layout.activity_main);
                        mTextMessage.setText(R.string.title_home);
                        return true;
                    case R.id.navigation_dashboard:
                        setContentView(R.layout.activity_maps);
                        mTextMessage.setText(R.string.title_dashboard);
                        return true;
                }
                return false;
            }
        });
    }*/
}
