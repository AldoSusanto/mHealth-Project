package com.mhealth.application;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


public class PasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);
    }
    public void setPassword(View view){
        EditText currPassInput = (EditText) findViewById(R.id.currPassInput);
        String currentPassword = currPassInput.getText().toString();

        EditText newPassInput1 = (EditText) findViewById(R.id.newPassInput1);
        String newPass1 = newPassInput1.getText().toString();

        EditText newPassInput2 = (EditText) findViewById(R.id.newPassInput2);
        String newPass2 = newPassInput2.getText().toString();

        String origPass = MainActivity.password; // this is the actual original password that we have

        if(newPass1.isEmpty() || currentPassword.isEmpty() || newPass2.isEmpty()){
            Toast.makeText(this, "Please fill all the required fields", Toast.LENGTH_LONG).show();
            return;
        }
        if(!newPass1.equals(newPass2)){
            Toast.makeText(this, "The new password fields aren't equal" , Toast.LENGTH_LONG).show();
            return;
        }
        if(!currentPassword.equals(origPass)){
            Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Password changed successfully", Toast.LENGTH_LONG).show();
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", newPass1);
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }

    public void back(View view) {
        onBackPressed();
    }
}
