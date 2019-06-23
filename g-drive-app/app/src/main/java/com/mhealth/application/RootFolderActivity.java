package com.mhealth.application;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class RootFolderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root_folder);
    }

    public void buttonPressed(View view) throws InterruptedException {
        EditText batchNumTemp = (EditText) findViewById(R.id.editText); // gets the value of the batch Number to create the folder
        String batchNum =  batchNumTemp.getText().toString();

        Toast prompt;

        // we gather the data as a string and ensure they aren't empty
        if(batchNum.isEmpty()){ //
            prompt = Toast.makeText(this, "Data incomplete, please fill all required data", Toast.LENGTH_LONG);
            prompt.show();
            return;
        }
        if(batchNum.length()>2){ //
            prompt = Toast.makeText(this, "Only two digit batch numbers are valid", Toast.LENGTH_LONG);
            prompt.show();
            return;
        }

        TextView pleaseWait = (TextView) findViewById(R.id.pleaseWait);
        if(pleaseWait.getVisibility() == View.INVISIBLE){
            pleaseWait.setVisibility(View.VISIBLE);
        }

        if (batchNum.length() < 2){
            batchNum = "0" + batchNum;
        }


        String rootName = "Study " + batchNum;
        FolderRequest.createRoot(this, rootName, new FolderRequest.FolderListener() {
            @Override
            public void onFolderCreated(Boolean success) {
                if (success){
                    Toast.makeText(RootFolderActivity.this, "Folder creation successful", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(RootFolderActivity.this, "Folder creation failed; please try again", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void back(View view) {
        onBackPressed();
    }
}
