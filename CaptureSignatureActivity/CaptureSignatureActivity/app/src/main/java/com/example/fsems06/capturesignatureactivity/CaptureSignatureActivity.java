package com.example.fsems06.capturesignatureactivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class CaptureSignatureActivity extends Activity {

    public static final int SIGNATURE_ACTIVITY = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button getSignature = (Button) findViewById(R.id.signature);
        getSignature.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(CaptureSignatureActivity.this, CaptureSignature.class);
                startActivityForResult(intent,SIGNATURE_ACTIVITY);
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch(requestCode) {
            case SIGNATURE_ACTIVITY:
                if (resultCode == RESULT_OK) {

                    Bundle bundle = data.getExtras();
                    String status  = bundle.getString("status");
                    if(status.equalsIgnoreCase("done")){
                        Toast toast = Toast.makeText(this, "Signature capture successful!", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.TOP, 105, 50);
                        toast.show();
                    }
                }
                break;
        }

    }

}