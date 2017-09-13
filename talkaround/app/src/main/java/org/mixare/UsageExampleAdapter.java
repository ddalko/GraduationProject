package org.mixare;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mixare.data.Json;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class UsageExampleAdapter extends AppCompatActivity {
    ListView listView;
    public static String[] imageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage_example_adapter);
        listView = (ListView)this.findViewById(R.id.listView);

        String myNumber = null;
        TelephonyManager mgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        try{
            myNumber = mgr.getLine1Number();
            myNumber = myNumber.replace("+82", "0");

        }catch(Exception e){}

        InsertData insertTask = new InsertData();
        insertTask.execute(myNumber);
    }

    //DB관련 클래스-------------------------------------------------------------
    private class InsertData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ArrayList<String> tempUrls = new ArrayList<String>();
            try{
                JSONArray results = new JSONArray(result);

                for(int i = 0; i < results.length(); ++i){
                    JSONObject tmp = results.getJSONObject(i);
                    tempUrls.add(tmp.get("url").toString());
                }
            }catch(JSONException e){
                e.printStackTrace();
            }

            imageUrl = new String[tempUrls.size()];
            for(int i = 0; i < tempUrls.size(); ++i){
                imageUrl[i] = tempUrls.get(i);
            }

            listView.setAdapter(new ImageListAdapter(UsageExampleAdapter.this, imageUrl));
        }

        @Override
        protected String doInBackground(String... params) {
            final String serverUrl = "http://220.95.88.213:22223/Timeline.php";
            String writer = (String)params[0];
            String postParameters = "writer=" + writer;
            Log.e("TAG", postParameters);

            try {
                URL url = new URL(serverUrl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                //httpURLConnection.setRequestProperty("content-type", "application/json");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line = null;

                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }

                bufferedReader.close();
                Log.e("TAG", sb.toString());
                return sb.toString();
            } catch (Exception e) {

                Log.d("timeline Error", "InsertData: Error ", e);

                return new String("Error: " + e.getMessage());
            }
        }
    }
}