package com.somoye.android.inappbilling;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;

import java.util.List;

public class PurchaseListener implements PurchasesUpdatedListener {

    Context context;
    BillingConnection billingConnection;
    public PurchaseListener(Context context) {
        this.context = context;
       billingConnection  = new BillingConnection(context);

    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {

        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

            if (purchases != null) {

                for (Purchase purchase : purchases) {

                  billingConnection.handlePurchase(purchase);
                }
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            if (purchases != null) {

                for (Purchase purchase : purchases) {


                    //showToast("You have have already make payment for iab " +
                    //  purchase.getSignature()
                    //);
                }
            }
        }
    }

}
