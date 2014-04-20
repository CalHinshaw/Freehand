package com.freehand.billing;

import android.util.Log;

import com.android.vending.billing.IabHelper;
import com.android.vending.billing.IabResult;
import com.android.vending.billing.Inventory;
import com.android.vending.billing.Purchase;

public class FreehandIabHelper {
	
	public static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwc8zJDwniUbdnqKiUtkMbrLomID1JaQ0cJLApsX7qbBGU" +
			"+78bwUwZ7JlFtBF/ITxstrYXwiiFmB3HKbQaGdTWf2TOb7TK0ysitlcBzHBRYNvD6bsgxtaPwREwT1hql6z+cPrxISyXvRCsvf/T+u9gNGkEkkZeg1SMOL+" +
			"ZhOViHCpzQbF6znNu10r771WDdOmv0Ta7W/Qi/icuKf8fKRWhgDh4AzVhmE2z34bGoAZEwkA7AxdedAgdnCe7R6MargdJS9EUh+5PqFsY5QhT115VUbBD2E" +
			"W8kcqp6wlj3HuxGdfzevok7fNBQ8qaxSEeWGFCv8dYVLQ4WBnXqIIAA/OSQIDAQAB";
	
	public static final String SKU_PRO = "android.test.purchased";
	
	
	
	public static void loadIAB (final IabHelper iabHelper, final ProStatusCallbackFn proStatusCallback) {
    	if (iabHelper == null) return;
    	
    	final IabHelper.QueryInventoryFinishedListener queryFinishedListener = new IabHelper.QueryInventoryFinishedListener() {
			@SuppressWarnings("unused")
			@Override
			public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
				Log.d("PEN", "Query inventory finished.");
				
	            // Have we been disposed of in the meantime? If so, quit.
	            if (iabHelper == null) {
	            	Log.d("PEN", "Our IabHelper was disposed of before setup finished.");
	            	return;
	            }
	            
	            // Did it a fail? If so, quit.
	            if (result.isFailure()) {
	                Log.d("PEN", "Failed to query inventory: " + result);
	                return;
	            }
	            
	            Log.d("PEN", "Query inventory was successful.");
	            
	            Purchase proPurchase = inventory.getPurchase(SKU_PRO);
	            if (proStatusCallback != null) {
	            	proStatusCallback.proStatusCallbackFn(proPurchase != null);
	            }
	            Log.d("PEN", "User is " + ((proPurchase != null) ? "PRO" : "NOT PRO"));
			}
		};
		
    	final IabHelper.OnIabSetupFinishedListener setupFinishedListener = new IabHelper.OnIabSetupFinishedListener() {
			@SuppressWarnings("unused")
			@Override
			public void onIabSetupFinished(IabResult result) {
				Log.d("PEN", "IabHelper setup finished");
				
	            if (!result.isSuccess()) {
	                Log.d("PEN", "There was a problem setting up in-app billing: " + result);
	                return;
	            }
	
	            // Have we been disposed of in the meantime? If so, quit.
	            if (iabHelper == null) {
	            	Log.d("PEN", "Our IabHelper was disposed of before setup finished.");
	            	return;
	            }
	
	            // IAB is fully set up. Now, let's get an inventory of stuff we own.
	            Log.d("PEN", "Setup successful. Querying inventory.");
	            iabHelper.queryInventoryAsync(queryFinishedListener);
			}
    	};
    	
    	iabHelper.startSetup(setupFinishedListener);
    }
    
//    public static void buyPro (final Activity activity, final int requestCode, final CallbackFn callback) {
//    	final IabHelper.OnIabPurchaseFinishedListener listener = new IabHelper.OnIabPurchaseFinishedListener() {
//			@Override
//			public void onIabPurchaseFinished(IabResult result, Purchase info) {
//				if (result.isSuccess()) {
//					hasProLisence = true;
//					callback.callbackFn(true);
//				} else {
//					callback.callbackFn(false);
//				}
//			}
//		};
//    	
//    	iabHelper.launchPurchaseFlow(activity, SKU_PRO, requestCode, listener);
//    }
    
    public interface ProStatusCallbackFn {
    	public void proStatusCallbackFn (final Boolean result);
    }
}