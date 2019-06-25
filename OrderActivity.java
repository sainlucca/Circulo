package com.circulo.circulo.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.circulo.circulo.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Random;

public class OrderActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS =0 ;
    Button sendBtn;
    EditText txtMessage;
    String selectedPhoneNo;
    String message;
    RadioButton regulerOrder, boronganOrder;
    String jsonResultRombeng;
    String tempResult;
    String userName, userAddress, rombengType;
    double userLat, userLong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        getSupportActionBar().setTitle("Order Now!");

        sendBtn = (Button) findViewById(R.id.btnSendSMS);
        txtMessage = (EditText) findViewById(R.id.editMessages);
        regulerOrder = (RadioButton) findViewById(R.id.orderReguler);
        boronganOrder = (RadioButton) findViewById(R.id.orderBorongan);

        Bundle extras = getIntent().getExtras();
        userName = extras.getString("Fullname");
        userAddress = extras.getString("Address");
        userLat = extras.getDouble("Lat");
        userLong = extras.getDouble("Long");

        sendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if(regulerOrder.isChecked() == true || boronganOrder.isChecked() == true) {
                    getRombeng(userLat, userLong);
                }
                else
                    Toast.makeText(getApplicationContext(), "Choose Rombeng Type First!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getRombeng(final double userLat_param, final double userLong_param) {

        class GetRombengAsync extends AsyncTask<String, Void, String> {
            @Override
            protected void onPreExecute() {
            };

            @Override
            protected String doInBackground(String... urls) {

                HttpGet httpGet = new HttpGet(urls[0]);
                HttpParams httpParameters = new BasicHttpParams();

                int timeoutSocket = 10000;
                HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

                DefaultHttpClient client = new DefaultHttpClient(httpParameters);

                StringBuilder builder = new StringBuilder();
                Log.i("urls[0]", urls[0]);
                try {
                    HttpResponse response = client.execute(httpGet);
                    StatusLine statusLine = response.getStatusLine();
                    int statusCode = statusLine.getStatusCode();
                    if (statusCode == 200) {
                        HttpEntity entity = response.getEntity();
                        InputStream content = entity.getContent();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                    }
                    else {
                        Log.e("failed", "Failed to download file");
                    }
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                } catch (ConnectTimeoutException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return builder.toString();
            }

            protected void onProgressUpdate(Integer... progress) {
                setProgress(progress[0]);
            }

            @Override
            protected void onPostExecute(String result) {

                jsonResultRombeng = result;
                getRombengResult();
            }
        }

        String serverurl = "mycirculo.com";
        GetRombengAsync task = new GetRombengAsync();
        String url = "http://"+serverurl+"/query.php?function=CheckCoord&userLat=" + userLat_param + "&userLong=" + userLong_param + "";
        task.execute(url);
    }


    /**
     * Set value from JSON
     */
    public void getRombengResult() {
        int i;

        JSONArray jsonArray;

        try {
            jsonArray = new JSONArray(jsonResultRombeng);

            double maxRadius = 0; //kilometers
            double rombengDistReg, rombengDistBorongan;
            String rombengName, rombengPhone1, rombengPhone2;

            ArrayList<String> rombengList = new ArrayList<String>();

            for (i = 0; i < jsonArray.length(); i++) {
                rombengName = String.valueOf(jsonArray.getJSONObject(i).getString("ROMBENG_NAME"));
                rombengPhone1 = String.valueOf(jsonArray.getJSONObject(i).getString("ROMBENG_PHONE1"));
                rombengPhone2 = String.valueOf(jsonArray.getJSONObject(i).getString("ROMBENG_PHONE2"));
                rombengDistReg =  Double.valueOf(jsonArray.getJSONObject(i).getString("DISTANCE_REGULER"));
                rombengDistBorongan = Double.valueOf(jsonArray.getJSONObject(i).getString("DISTANCE_BORONGAN"));

                if(regulerOrder.isChecked() == true) {
                    maxRadius = 5;
                    rombengType = regulerOrder.getText().toString();
                    if(rombengDistReg <= maxRadius)
                    {
                        rombengList.add(rombengName + "§" +
                                rombengPhone1 + "§" +
                                rombengPhone2 + "§" +
                                rombengDistReg);
                    }
                    else
                    {
                        rombengList.add("");
                    }
                }
                else {
                    maxRadius = 10;
                    rombengType = boronganOrder.getText().toString();
                    if(rombengDistBorongan <= maxRadius)
                    {
                        rombengList.add(rombengName + "§" +
                                rombengPhone1 + "§" +
                                rombengPhone2 + "§" +
                                rombengDistBorongan);
                    }
                    else
                    {
                        rombengList.add("");
                    }
                }
            }

            Random rand = new Random();
            int n = rand.nextInt(jsonArray.length());
            tempResult = rombengList.get(n).toString();

            sendSMSMessage();

        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("catch error ", e.getMessage());
        }
    }

    protected void sendSMSMessage() {

        final String rombengName, rombengPhone1, rombengPhone2;
        String[] data = tempResult.split("§");

        for (int j = 0; j < data.length; j++)
            Log.d("data[" + j + "] ->", data[j]);
        rombengName = data[0];
        rombengPhone1 = data[1];
        rombengPhone2 = data[2];

        new AlertDialog.Builder(this)
                .setTitle("Sending Order messages")
                .setMessage("Circulo apps would like to send a message. This is may cause charges on your mobile account. Do you want to continue?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        new AlertDialog.Builder(OrderActivity.this)
                                .setTitle("Confirmation")
                                .setMessage("Are you sure to send this order?")
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        selectedPhoneNo = rombengPhone1;
                                        //selectedPhoneNo = "+6281939363737";
                                        message = "Mas/Mbak " + rombengName +
                                                ", saya " + userName + " yang beralamat di " + userAddress +
                                                ", ingin mengunakan jasa " + rombengType + ".\n" +
                                                "NB : " + txtMessage.getText().toString();

                                        if (ContextCompat.checkSelfPermission(OrderActivity.this,
                                                Manifest.permission.SEND_SMS)
                                                != PackageManager.PERMISSION_GRANTED) {
                                            if (ActivityCompat.shouldShowRequestPermissionRationale(OrderActivity.this,
                                                    Manifest.permission.SEND_SMS)) {
                                            } else {
                                                ActivityCompat.requestPermissions(OrderActivity.this,
                                                        new String[]{Manifest.permission.SEND_SMS},
                                                        MY_PERMISSIONS_REQUEST_SEND_SMS);
                                            }
                                        }
                                        else
                                        {
                                            SmsManager smsManager = SmsManager.getDefault();
                                            smsManager.sendTextMessage(selectedPhoneNo, null, message, null, null);
                                            Toast.makeText(getApplicationContext(), "SMS sent.",
                                                    Toast.LENGTH_LONG).show();

                                            Log.d("message : ", message);
                                            Log.d("telp : ", selectedPhoneNo);
                                        }
                                    }})
                                .setNegativeButton(android.R.string.no, null).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(selectedPhoneNo, null, message, null, null);
                    Toast.makeText(getApplicationContext(), "SMS sent.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "SMS failed, please try again.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                backToHomePage();
            }
        }, 1);
    }

    private void backToHomePage() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Order!")
                .setMessage("Do you want to cancel your order?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent backToHome = new Intent(OrderActivity.this, HomeActivity.class);
                        startActivity(backToHome);
                        finish();
                    }})
                .setNegativeButton(android.R.string.no, null).show();
    }
}
