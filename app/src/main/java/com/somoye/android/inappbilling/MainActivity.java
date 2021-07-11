package com.somoye.android.inappbilling;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;


//import org.apache.commons.codec.digest.DigestUtils;
//import org.apache.http.NameValuePair;
//import org.apache.http.client.utils.URLEncodedUtils;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClientBuilder;
//import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    Button btn;
    BillingClient billingClient;
    Context context;
    boolean isSub = true;



    //private PurchasesUpdatedListener purchasesUpdatedListener;
    //PurchaseListener purchaseListener;
    BillingConnection billingConnection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btn = findViewById(R.id.buy);



        /*You can test it live*/
        btn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {

               switchInAppPurchase();

                //startBillingConnection();
            }
        });

        //Library initializer
       billingConnection = BillingConnection.getInstance();
       billingConnection.connect(this);


    }

    public void switchInAppPurchase(){

        List<String> pro = new ArrayList<>();

        String productId = "sub_test";
        String purInapp = "remove_ads_test";
        String skutype_Subs = BillingClient.SkuType.SUBS;
        String skutype_InApp = BillingClient.SkuType.INAPP;

        pro.add(productId);
        pro.add(purInapp);

        if (billingConnection.isConnected()){

            if (isSub){
                pro.remove(purInapp);
                billingConnection.queryProducts(this,pro,skutype_Subs);  //comment this if testing Inapp
                isSub = false;
                btn.setText("Purchase");
                btn.setBackgroundColor(getResources().getColor(R.color.teal_200));
            } else {
                pro.remove(productId);
                billingConnection.queryProducts(this,pro,skutype_InApp);
                isSub = true;
                btn.setText("Subscribe");
                btn.setBackgroundColor(getResources().getColor(R.color.purple_200));

            }


        } else {
            showToast("Not Connected");
        }

    }





    //Moved to the lib
    public void startBillingConnection() {

       billingConnection.getBillingClient().startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    List<String> skuList = new ArrayList<>();
                    skuList.add("premium_upgrade");
                    skuList.add("gas");
                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
                    billingClient.querySkuDetailsAsync(params.build(),
                            new SkuDetailsResponseListener() {
                                @Override
                                public void onSkuDetailsResponse(BillingResult billingResult,
                                                                 List<SkuDetails> skuDetailsList) {
                                    // Process the result.
                                    if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK)
                                    {
                                        if(skuDetailsList.size() > 0){
                                            String result = skuDetailsList.toString();
                                            alertDialog(MainActivity.this,"Sku Details",result);
                                        }
                                    }
                                }
                            });
                   // queryProducts();
                    Toast.makeText(MainActivity.this, "Starting Connection...", Toast.LENGTH_SHORT).show();
                }

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
                    showToast("service disconnected");
                }

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE) {
                    showToast("unavailable service");
                }

            }

            @Override
            public void onBillingServiceDisconnected() {
                Toast.makeText(MainActivity.this, "Disconnecting...", Toast.LENGTH_SHORT).show();

            }
        });

    }

    public void alertDialog(Context context, String title, String message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.create();
        builder.show();
    }



    public void showToast(String toast_message) {
        Toast.makeText(this, toast_message, Toast.LENGTH_SHORT).show();
    }


}