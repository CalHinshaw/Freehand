package com.freehand.billing;

import android.content.Context;
import android.util.Log;

import com.android.vending.billing.IabHelper;
import com.android.vending.billing.IabResult;
import com.android.vending.billing.Inventory;
import com.android.vending.billing.Purchase;

public class IabSingleton {
	
	private static final String SKU_PRO = "pro";
	
	private static IabHelper iabHelper;
	private static boolean hasProLisence = false;
	
	public static void loadIAP (final Context context) {
    	final String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwc8zJDwniUbdnqKiUtkMbrLomID1JaQ0cJLApsX7qbBGU" +
    			"+78bwUwZ7JlFtBF/ITxstrYXwiiFmB3HKbQaGdTWf2TOb7TK0ysitlcBzHBRYNvD6bsgxtaPwREwT1hql6z+cPrxISyXvRCsvf/T+u9gNGkEkkZeg1SMOL+" +
    			"ZhOViHCpzQbF6znNu10r771WDdOmv0Ta7W/Qi/icuKf8fKRWhgDh4AzVhmE2z34bGoAZEwkA7AxdedAgdnCe7R6MargdJS9EUh+5PqFsY5QhT115VUbBD2E" +
    			"W8kcqp6wlj3HuxGdfzevok7fNBQ8qaxSEeWGFCv8dYVLQ4WBnXqIIAA/OSQIDAQAB";
    	
    	iabHelper = new IabHelper(context, base64EncodedPublicKey);
    	
    	final IabHelper.QueryInventoryFinishedListener queryFinishedListener = new IabHelper.QueryInventoryFinishedListener() {
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
	            hasProLisence = (proPurchase != null);
	            Log.d("PEN", "User is " + (hasProLisence ? "PRO" : "NOT PRO"));
			}
		};
		
    	final IabHelper.OnIabSetupFinishedListener setupFinishedListener = new IabHelper.OnIabSetupFinishedListener() {
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
    
    public static boolean getIsPro () {
    	return hasProLisence;
    }
}