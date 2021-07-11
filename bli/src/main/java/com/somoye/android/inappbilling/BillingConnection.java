package com.somoye.android.inappbilling;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchasesResult;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.List;

public class BillingConnection {

    BillingClient billingClient;
    Context context;
    static boolean isConnected = false;
    static boolean isPurchased = false;
    static boolean isSubscribe = false;

    Handler handler = new Handler(Looper.getMainLooper());

    interface initializeCallbacks {
        void onPurchaseSuccessful(BillingClient.BillingResponseCode billingResponseCode);


    }

    public Handler getHandler() {
        return handler;
    }

    public BillingConnection(Context context) {
        this.context = context;
    }



    private static BillingConnection instance = null;
    private BillingConnection() {
    }

    public static BillingConnection getInstance() {
        if (instance == null){
            instance = new BillingConnection();
        }
        return instance;
    }

    public BillingClient getBillingClient() {
        return billingClient;
    }

    public void setBillingClient(BillingClient billingClient) {
        this.billingClient = billingClient;
    }

    public void connect(Context context) {

        billingClient = BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener(purchasesUpdatedListener)
                .build();

        setBillingClient(billingClient);

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    //queryProducts();
                    isConnected = true;
                    Toast.makeText(context, "Starting Connection...", Toast.LENGTH_SHORT).show();
                }


                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE) {
                    showToast(context,"unavailable service");
                }

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                    //showToast(context,"Canceling connection");
                    Toast.makeText(context, "Canceling connection", Toast.LENGTH_SHORT).show();

                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                showToast(context,"Disconnecting...");
            }
        });
    }

    public void showToast(Context context,String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

     boolean isConnected() {
        return getBillingClient().isReady();
    }



    void handlePurchase(Purchase purchase) {
        // Purchase retrieved from BillingClient#queryPurchasesAsync or your PurchasesUpdatedListener.
        //Purchase purchase =

        // Verify the purchase.
        // Ensure entitlement was not already granted for this purchaseToken.
        // Grant entitlement to the user.

        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();


        ConsumeResponseListener listener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // Handle the success of the consume operation.
                    getHandler().post(new Runnable() {
                        @Override
                        public void run() {

                            showToast(context,"You have completed payment for iab " + purchaseToken);
                        }
                    });

                }
            }
        };


        //Acknowledging purchase
        AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {

            }
        };

        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
               getBillingClient().acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
            }


            getBillingClient().consumeAsync(consumeParams, listener);
        }


    }


    PurchasesUpdatedListener purchasesUpdatedListener = new
            PurchasesUpdatedListener() {
                @Override
                public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {

                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                        if (purchases != null) {

                            for (Purchase purchase : purchases) {

                                handlePurchase(purchase);

                                getHandler().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        String json = purchase.getOriginalJson();
                                        //Toast.makeText(context,"Success",Toast.LENGTH_LONG).show();

                                        Log.d("Purchase; ",json);
                                    }
                                });
                            }
                        }
                    } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                        if (purchases != null) {

                            for (Purchase purchase : purchases) {



                                getHandler().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        String json = purchase.getOriginalJson();
                                        Toast.makeText(getInstance().context,"Purchased: "+json,Toast.LENGTH_LONG).show();
                                        Log.d("Already Purchase; ",json);
                                    }
                                });
                                //showToast("You have have already make payment for iab " +
                                //  purchase.getSignature()
                                //);
                            }
                        }
                    }

                }
            };

    boolean isPurchasedAfterConnected(String productId){


            PurchasesResult purchasesResult = getBillingClient().queryPurchases(productId);
           List<Purchase> purchaseList =  purchasesResult.getPurchasesList();

           // int response = purchasesResult.getResponseCode();
            //for (int i = 0 ; i < purchaseList.size();i++){
            //if (response == BillingClient.BillingResponseCode.OK)
            //{
            //}
            if (purchaseList != null){
                Log.d("Purchase List; ",purchaseList.toString());

                int purchaseState = purchaseList.get(0).getPurchaseState();
                if (purchaseState == Purchase.PurchaseState.PURCHASED){
                    isPurchased = true;
                    return isPurchased;
                } else {
                    isPurchased = false;
                    return isPurchased;
                }
            } else {
                isPurchased = false;
                return isPurchased;
            }
    }


    public void purchaseInAppProducts(Activity activity, List<String> skuList) {
        //skuList = new ArrayList<>();
        //skuList.add("remove_ads_test");
        //skuList.add("sub_test");
        //skuList.add("gas");
        if(isConnected()) {
            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            if (skuList.size()>0 && !skuList.isEmpty()) {
                params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
                //params.build();
                getBillingClient().querySkuDetailsAsync(params.build(),
                        new SkuDetailsResponseListener() {
                            @Override
                            public void onSkuDetailsResponse(BillingResult billingResult,
                                                             List<SkuDetails> skuDetailsList) {
                                // Process the result.
                                //Toast.makeText(activity, "Processing Results...", Toast.LENGTH_SHORT).show();

                                if (skuDetailsList.size()>0 && !skuDetailsList.isEmpty()) {
                                    SkuDetails skuDetails = null;
                                    for (int i = 0; i < skuDetailsList.size(); i++) {
                                        skuDetails = skuDetailsList.get(i);
                                        skuDetails.getPrice();
                                        skuDetails.getTitle();
                                        skuDetails.getDescription();
                                        skuDetails.getSku();
                                    }


                                    Handler handler = new Handler(Looper.getMainLooper());


                                    SkuDetails finalSkuDetails = skuDetails;
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {

                                            handler.post(new Runnable() {
                                                @Override
                                                public void run() {

                                                    // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
                                                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                                            .setSkuDetails(finalSkuDetails)
                                                            .build();
                                                    int responseCode = getBillingClient().launchBillingFlow(activity, billingFlowParams).getResponseCode();
                                                    String responseMessage = "Billing response; ";

                                                    if (responseCode == BillingClient.BillingResponseCode.OK) {
                                                     //   showToast(responseMessage + "Ok");
                                                    }
                                                    if (responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                                                       // showToast(responseMessage + "Unavailable billing");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.DEVELOPER_ERROR) {
                                                      //  showToast(responseMessage + "developer");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED) {
                                                      //  showToast(responseMessage + "Feature not support");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.ERROR) {
                                                      //  showToast(responseMessage + "error");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.SERVICE_TIMEOUT) {
                                                    //    showToast(responseMessage + "time out");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                                                      //  showToast(responseMessage + "owned");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
                                                    //    showToast(responseMessage + "disconnected");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE) {
                                                      //  showToast(responseMessage + "service not availabler");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                                                     //   showToast(responseMessage + "user cancel");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                                                     //   showToast(responseMessage + "not available for purchase");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.ITEM_NOT_OWNED) {
                                                     //   showToast(responseMessage + "not owned");
                                                    }
                                                    // Handle the result.
                                                }
                                            });
                                        }
                                    }).start();
                                }
                                else {
                                    Log.d("onSkuResponse: ", "product id mismatch with Product type");
                                //    showToast("product id mismatch with Product type");
                                }

                                //  try {

                                //     SkuDetails skuDetails = new SkuDetails("remove_ads_test");

                            }
                        });
            } else {
                Log.d("onProductId: ", "product id not specified");
            }

        }

    }



    public void subscribe(Activity activity, List<String> skuList) {
        //skuList = new ArrayList<>();
        //skuList.add("remove_ads_test");
        //skuList.add("sub_test");
        //skuList.add("gas");
        if(isConnected()) {
            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            if (skuList.size()>0 && !skuList.isEmpty()) {
                params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS);
                //params.build();
                getBillingClient().querySkuDetailsAsync(params.build(),
                        new SkuDetailsResponseListener() {
                            @Override
                            public void onSkuDetailsResponse(BillingResult billingResult,
                                                             List<SkuDetails> skuDetailsList) {
                                // Process the result.
                                //Toast.makeText(activity, "Processing Results...", Toast.LENGTH_SHORT).show();

                                if (skuDetailsList.size()>0 && !skuDetailsList.isEmpty()) {
                                    SkuDetails skuDetails = null;
                                    for (int i = 0; i < skuDetailsList.size(); i++) {
                                        skuDetails = skuDetailsList.get(i);
                                        skuDetails.getPrice();
                                        skuDetails.getTitle();
                                        skuDetails.getDescription();
                                        skuDetails.getSku();
                                    }


                                    Handler handler = new Handler(Looper.getMainLooper());


                                    SkuDetails finalSkuDetails = skuDetails;
                                   /* new Thread(new Runnable() {
                                        @Override
                                        public void run() {

                                        }
                                    }).start();*/

                                    getHandler().post(new Runnable() {
                                        @Override
                                        public void run() {

                                            // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
                                            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                                    .setSkuDetails(finalSkuDetails)
                                                    .build();
                                            int responseCode = getBillingClient().launchBillingFlow(activity, billingFlowParams).getResponseCode();
                                            String responseMessage = "Billing response; ";

                                            if (responseCode == BillingClient.BillingResponseCode.OK) {
                                                 showToast(activity,responseMessage + "Ok");
                                               // Toast.makeText(activity, responseMessage + "Ok", Toast.LENGTH_SHORT).show();

                                            }
                                            if (responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                                                showToast(activity,responseMessage + "Unavailable billing");
                                            }

                                            if (responseCode == BillingClient.BillingResponseCode.DEVELOPER_ERROR) {
                                                // showToast(responseMessage + "developer");
                                            }

                                            if (responseCode == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED) {
                                                //   showToast(context,responseMessage + "Feature not support");
                                            }

                                            if (responseCode == BillingClient.BillingResponseCode.ERROR) {
                                                //     showToast(responseMessage + "error");
                                            }

                                            if (responseCode == BillingClient.BillingResponseCode.SERVICE_TIMEOUT) {
                                                //       showToast(responseMessage + "time out");
                                            }

                                            if (responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                                                //         showToast(responseMessage + "owned");
                                                isSubscribe = true;
                                            }

                                            if (responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
                                                //           showToast(responseMessage + "disconnected");
                                            }

                                            if (responseCode == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE) {
                                                //             showToast(responseMessage + "service not availabler");
                                            }

                                            if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                                                //               showToast(responseMessage + "user cancel");
                                            }

                                            if (responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                                                //                 showToast(responseMessage + "not available for purchase");
                                            }

                                            if (responseCode == BillingClient.BillingResponseCode.ITEM_NOT_OWNED) {
                                                //                   showToast(responseMessage + "not owned");
                                            }
                                            // Handle the result.
                                        }
                                    });

                                }
                                else {
                                    Log.d("onSkuResponse: ", "product id mismatch with Product type");
                                   // showToast("product id mismatch with Product type");
                                }

                                //  try {

                                //     SkuDetails skuDetails = new SkuDetails("remove_ads_test");

                            }
                        });
            } else {
                Log.d("onProductId: ", "product id not specified");
            }

        } else {
            isSubscribe = false;
        }

    }


    public boolean isInAppProductsPurchased(Activity activity, String productId) {
        List<String> skuList = new ArrayList<>();
        //skuList.add("remove_ads_test");
        //skuList.add("sub_test");
        skuList.add(productId);

        if(isConnected()) {
            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
                //params.build();
                getBillingClient().querySkuDetailsAsync(params.build(),
                        new SkuDetailsResponseListener() {
                            @Override
                            public void onSkuDetailsResponse(BillingResult billingResult,
                                                             List<SkuDetails> skuDetailsList) {
                                // Process the result.
                                //Toast.makeText(activity, "Processing Results...", Toast.LENGTH_SHORT).show();

                                if (skuDetailsList.size()>0 && !skuDetailsList.isEmpty()) {
                                    SkuDetails skuDetails = null;
                                    for (int i = 0; i < skuDetailsList.size(); i++) {
                                        skuDetails = skuDetailsList.get(i);
                                        skuDetails.getPrice();
                                        skuDetails.getTitle();
                                        skuDetails.getDescription();
                                        skuDetails.getSku();
                                      // productId = skuDetailsList.get(i).getSku().contains(productId);
                                    }

                                    SkuDetails finalSkuDetails = skuDetails;



                                                    // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
                                                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                                            .setSkuDetails(finalSkuDetails)
                                                            .build();
                                                    //int responseCode = getBillingClient().launchBillingFlow(activity, billingFlowParams).getResponseCode();
                                                    int responseCode = billingResult.getResponseCode();
                                                    String responseMessage = "Billing response; ";

                                                    if (responseCode == BillingClient.BillingResponseCode.OK) {
                                                      //  showToast(responseMessage + "Ok");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.ERROR) {
                                                        //showToast(responseMessage + "error");
                                                    }


                                                    if (responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                                                       // showToast(responseMessage + "owned");
                                                        isPurchased = true;

                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
                                                      //  showToast(responseMessage + "disconnected");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE) {
                                                      //  showToast(responseMessage + "service not availabler");
                                                    }

                                                    // Handle the result.

                                }
                                else {
                                    Log.d("onSkuResponse: ", "product id mismatch with Product type");
                            //        showToast("product id mismatch with Product type");
                                }

                                //  try {

                                //     SkuDetails skuDetails = new SkuDetails("remove_ads_test");

                            }
                        });


        }

        return isPurchased;
    }

    /**
     * Checks purchase signature validity
     */
    /*private boolean isPurchaseSignatureValid(Purchase purchase) {
        return Security.verifyPurchase(base64Key, purchase.getOriginalJson(), purchase.getSignature());
    }*/

    public void queryProducts(Activity activity, List<String> skuList, String skuType) {
        //skuList = new ArrayList<>();
        //skuList.add("remove_ads_test");
        //skuList.add("sub_test");
        //skuList.add("gas");
        if(isConnected()) {
            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            if (skuList.size()>0 && !skuList.isEmpty()) {
                params.setSkusList(skuList).setType(skuType);
                //params.build();
                getBillingClient().querySkuDetailsAsync(params.build(),
                        new SkuDetailsResponseListener() {
                            @Override
                            public void onSkuDetailsResponse(BillingResult billingResult,
                                                             List<SkuDetails> skuDetailsList) {
                                // Process the result.
                                //Toast.makeText(activity, "Processing Results...", Toast.LENGTH_SHORT).show();

                                if (skuDetailsList.size()>0 && !skuDetailsList.isEmpty()) {
                                    SkuDetails skuDetails = null;
                                    for (int i = 0; i < skuDetailsList.size(); i++) {
                                        skuDetails = skuDetailsList.get(i);
                                        skuDetails.getPrice();
                                        skuDetails.getTitle();
                                        skuDetails.getDescription();
                                        skuDetails.getSku();
                                    }


                                    Handler handler = new Handler(Looper.getMainLooper());


                                    SkuDetails finalSkuDetails = skuDetails;
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {

                                            handler.post(new Runnable() {
                                                @Override
                                                public void run() {

                                                    // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
                                                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                                            .setSkuDetails(finalSkuDetails)
                                                            .build();
                                                    int responseCode =getBillingClient().launchBillingFlow(activity, billingFlowParams).getResponseCode();
                                                    String responseMessage = "Billing response; ";

                                                    if (responseCode == BillingClient.BillingResponseCode.OK) {
                                                        showToast(activity,responseMessage + "Ok");
                                                    }
                                                    if (responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                                                        showToast(activity,responseMessage + "Unavailable billing");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.DEVELOPER_ERROR) {
                                                        showToast(activity,responseMessage + "developer");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED) {
                                                        showToast(activity,responseMessage + "Feature not support");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.ERROR) {
                                                        showToast(activity,responseMessage + "error");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.SERVICE_TIMEOUT) {
                                                        showToast(activity,responseMessage + "time out");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                                                        showToast(activity,responseMessage + "owned");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
                                                        showToast(activity,responseMessage + "disconnected");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE) {
                                                        showToast(activity,responseMessage + "service not availabler");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                                                        showToast(activity,responseMessage + "user cancel");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                                                        showToast(activity,responseMessage + "not available for purchase");
                                                    }

                                                    if (responseCode == BillingClient.BillingResponseCode.ITEM_NOT_OWNED) {
                                                        showToast(activity,responseMessage + "not owned");
                                                    }
                                                    // Handle the result.
                                                }
                                            });
                                        }
                                    }).start();
                                }
                                else {
                                    Log.d("onSkuResponse: ", "product id mismatch with Product type");
                                    showToast(activity,"product id mismatch with Product type");
                                }

                                //  try {

                                //     SkuDetails skuDetails = new SkuDetails("remove_ads_test");

                            }
                        });
            } else {
                Log.d("onProductId: ", "product id not specified");
            }

        }

    }

    public void showToast(Activity activity,String toast_message) {
        Toast.makeText(activity, toast_message, Toast.LENGTH_SHORT).show();
    }
}
