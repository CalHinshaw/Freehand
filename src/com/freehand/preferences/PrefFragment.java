package com.freehand.preferences;

import com.android.vending.billing.IabHelper;
import com.android.vending.billing.IabResult;
import com.android.vending.billing.Purchase;
import com.android.vending.billing.Inventory;
import com.calhounroberthinshaw.freehand.R;
import com.freehand.billing.FreehandIabHelper;

import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.InputType;
import android.util.Log;
import android.widget.Toast;

public class PrefFragment extends PreferenceFragment {
	
	private IabHelper iabHelper;
	private boolean isPro = false;
	
    @Override
    public void onCreate (Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	addPreferencesFromResource(R.xml.preferences);
    	
    	EditTextPreference pressure = (EditTextPreference)findPreference("pressure_sensitivity");
    	pressure.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    	
    	EditTextPreference zoom = (EditTextPreference)findPreference("zoom_threshold");
    	zoom.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    	
    	// IAB stuff
    	final Preference proPref = this.findPreference("pro_preference");
    	iabHelper = new IabHelper(this.getActivity(), FreehandIabHelper.getKey());
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
					consumeProForTesting(proPref);
					Toast.makeText(getActivity(), "Thanks for buying Pro, your support really helps!", Toast.LENGTH_LONG).show();
				} else {
					final IabHelper.OnIabPurchaseFinishedListener listener = new IabHelper.OnIabPurchaseFinishedListener() {
						@Override
						public void onIabPurchaseFinished(IabResult result, Purchase info) {
							Log.d("PEN", "purchase finished, result is " + result.isSuccess());
							isPro = isPro || result.isSuccess();
							proPref.setTitle(isPro ? "Thanks for buying PRO!" : "Get Freehand Pro!");
						}
					};
					
					iabHelper.launchPurchaseFlow(getActivity(), FreehandIabHelper.SKU_PRO, 0, listener);
				}
				
				return true;
			}
		});
    }
    
    private void consumeProForTesting (final Preference proPref) {
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
    }
    
    @Override
    public void onActivityResult (final int requestCode, final int resultCode, final Intent data) {
    	if (iabHelper == null || !iabHelper.handleActivityResult(requestCode, resultCode, data)) {
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
}