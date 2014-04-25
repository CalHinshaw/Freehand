package com.freehand.billing;

import android.util.Log;

import com.android.vending.billing.IabHelper;
import com.android.vending.billing.IabResult;
import com.android.vending.billing.Inventory;
import com.android.vending.billing.Purchase;

public class FreehandIabHelper {
	private static final String PK1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwc8zJDwniUbdn";
	private static final String PK2 = "qKiUtkMbrLomID1JaQ0cJLApsX7qbBGU+78bwUwZ7JlFtBF/ITxstrYXw";
	private static final String PK3 = "iiFmB3HKbQaGdTWf2TOb7TK0ysitlcBzHBRYNvD6bsgxtaPwREwT1hql6";
	private static final String PK4 = "z+cPrxISyXvRCsvf/T+u9gNGkEkkZeg1SMOL+ZhOViHCpzQbF6znNu10r";
	private static final String PK5 = "771WDdOmv0Ta7W/Qi/icuKf8fKRWhgDh4AzVhmE2z34bGoAZEwkA7Axde";
	private static final String PK6 = "dAgdnCe7R6MargdJS9EUh+5PqFsY5QhT115VUbBD2EW8kcqp6wlj3HuxG";
	private static final String PK7 = "dfzevok7fNBQ8qaxSEeWGFCv8dYVLQ4WBnXqIIAA/OSQIDAQAB";
	
	public static final String SKU_PRO = "pro";
	
	public static String getKey () {
		return PK1+PK2+PK3+PK4+PK5+PK6+PK7;
	}
	
	public static void loadIAB (final IabHelper iabHelper, final ProStatusCallbackFn proStatusCallback) {
    	if (iabHelper == null) return;
    	
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
	            queryPro(iabHelper, proStatusCallback);
			}
    	};
    	
    	iabHelper.startSetup(setupFinishedListener);
    }
	
	public static void queryPro (final IabHelper iabHelper, final ProStatusCallbackFn proStatusCallback) {
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
	            if (proStatusCallback != null) {
	            	proStatusCallback.proStatusCallbackFn(proPurchase != null);
	            }
	            Log.d("PEN", "User is " + ((proPurchase != null) ? "PRO" : "NOT PRO"));
			}
		};
		
		iabHelper.queryInventoryAsync(queryFinishedListener);
	}
    
    public interface ProStatusCallbackFn {
    	public void proStatusCallbackFn (final Boolean result);
    }
}