package com.freehand.preferences;

import com.android.vending.billing.IabHelper;
import com.android.vending.billing.IabResult;
import com.android.vending.billing.Inventory;
import com.android.vending.billing.Purchase;
import com.calhounroberthinshaw.freehand.R;
import com.freehand.billing.FreehandIabHelper;

import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.InputType;
import android.util.Log;

public class PrefActivity extends PreferenceActivity {
	
	private IabHelper iabHelper;
	private boolean isPro = false;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle("Settings");
		
		addPreferencesFromResource(R.xml.preferences);
    	
    	EditTextPreference pressure = (EditTextPreference)findPreference("pressure_sensitivity");
    	pressure.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    	
    	EditTextPreference zoom = (EditTextPreference)findPreference("zoom_threshold");
    	zoom.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    	
    	initIab();
	}
	
    @Override
    public void onActivityResult (final int requestCode, final int resultCode, final Intent data) {
    	if (iabHelper == null || !iabHelper.handleActivityResult(requestCode, resultCode, data)) {
    		Log.d("PEN", "purchase NOT handled");
    		super.onActivityResult(requestCode, resultCode, data);
    	}
    }
    
    @Override
    public void onDestroy () {
    	super.onDestroy();
    	if (iabHelper != null) {
    		iabHelper.dispose();
    	}
    }
    
    private void initIab () {
    	// IAB stuff
    	@SuppressWarnings("deprecation")
		final Preference proPref = this.findPreference("pro_preference");
    	iabHelper = new IabHelper(this, FreehandIabHelper.PUBLIC_KEY);
    	final FreehandIabHelper.ProStatusCallbackFn proCallback = new FreehandIabHelper.ProStatusCallbackFn() {
			@Override
			public void proStatusCallbackFn(Boolean result) {
				isPro = result;
				proPref.setTitle(result ? "Thanks for buying PRO!" : "Get Freehand Pro!");
			}
		};
    	
    	FreehandIabHelper.loadIAB(iabHelper, proCallback);
    	
		proPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			public boolean onPreferenceClick(final Preference preference) {
				
				if (isPro == true) {
					Log.d("PEN", "consuming pro");
					
					final IabHelper.OnConsumeFinishedListener consumeListener = new IabHelper.OnConsumeFinishedListener() {
						@Override
						public void onConsumeFinished(Purchase purchase, IabResult result) {
							if (result.isSuccess() == true) {
								Log.d("PEN", "Pro consumed");
								isPro = false;
							}
							proPref.setTitle(isPro ? "Thanks for buying PRO!" : "Get Freehand Pro!");
						}
					};
					
					IabHelper.QueryInventoryFinishedListener queryListener = new IabHelper.QueryInventoryFinishedListener() {
						@Override
						public void onQueryInventoryFinished(IabResult result, Inventory inv) {
							if (result.isSuccess()) {
								iabHelper.consumeAsync(inv.getPurchase(FreehandIabHelper.SKU_PRO), consumeListener);
								
							}
						}
					};
					
					iabHelper.queryInventoryAsync(queryListener);
					
					return true;
				}
				
		    	final IabHelper.OnIabPurchaseFinishedListener listener = new IabHelper.OnIabPurchaseFinishedListener() {
					@Override
					public void onIabPurchaseFinished(IabResult result, Purchase info) {
						Log.d("PEN", "purchase finished, result is " + result.isSuccess());
						isPro = isPro || result.isSuccess();
						proPref.setTitle(isPro ? "Thanks for buying PRO!" : "Get Freehand Pro!");
					}
				};
				
				iabHelper.launchPurchaseFlow(PrefActivity.this, FreehandIabHelper.SKU_PRO, 0, listener);
				
				return true;
			}
		});
    }
}