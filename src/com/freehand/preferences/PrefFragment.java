package com.freehand.preferences;

import com.android.vending.billing.IabHelper;
import com.android.vending.billing.IabResult;
import com.android.vending.billing.Purchase;
import com.calhounroberthinshaw.freehand.R;
import com.freehand.billing.FreehandIabHelper;

import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.InputType;
import android.widget.Toast;

public class PrefFragment extends PreferenceFragment {
    @Override
    public void onCreate (Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	addPreferencesFromResource(R.xml.preferences);
    	
    	FreehandIabHelper.updatePurchases(this.getActivity());
    	
    	EditTextPreference pressure = (EditTextPreference)findPreference("pressure_sensitivity");
    	pressure.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    	
    	EditTextPreference zoom = (EditTextPreference)findPreference("zoom_threshold");
    	zoom.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    	
    	EditTextPreference xThreshold = (EditTextPreference)findPreference("x_threshold");
    	xThreshold.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    	
    	EditTextPreference yThreshold = (EditTextPreference)findPreference("y_threshold");
    	yThreshold.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    	
    	// IAB stuff
    	final Preference proPref = this.findPreference("pro_preference");
    	proPref.setTitle(FreehandIabHelper.getProStatus(this.getActivity()) ? "Thanks for buying PRO!" : "Get Freehand Pro!");
    	
		proPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			public boolean onPreferenceClick(final Preference preference) {
				
				if (FreehandIabHelper.getProStatus(PrefFragment.this.getActivity()) == false) {
					final IabHelper.OnIabPurchaseFinishedListener listener = new IabHelper.OnIabPurchaseFinishedListener() {
						@Override
						public void onIabPurchaseFinished(IabResult result, Purchase info) {
							proPref.setTitle(FreehandIabHelper.getProStatus(PrefFragment.this.getActivity()) ? "Thanks for buying PRO!" : "Get Freehand Pro!");
						}
					};
					
					FreehandIabHelper.buyPro(PrefFragment.this.getActivity(), listener);
				} else {
					Toast.makeText(getActivity(), "Your support really helps!", Toast.LENGTH_LONG).show();
				}
				
				return true;
			}
		});
		
//		// DEBUG, SHOULD BE COMMENTED OUT BEFORE DEPLOYING
//		final Preference debug = this.findPreference("debug");
//		debug.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//			
//			@Override
//			public boolean onPreferenceClick(Preference preference) {
//				consumeProForTesting();
//				return true;
//			}
//		});
    }
    
//    private void consumeProForTesting () {
//    	Log.d("PEN", "starting consuming pro");
//		
//    	final IabHelper iabHelper = new IabHelper(this.getActivity(), FreehandIabHelper.getKey());
//    	
//		final IabHelper.OnConsumeFinishedListener consumeListener = new IabHelper.OnConsumeFinishedListener() {
//			@Override
//			public void onConsumeFinished(Purchase purchase, IabResult result) {
//				if (result.isSuccess() == true) {
//					Log.d("PEN", "Pro consumed");
//					FreehandIabHelper.updatePurchases(PrefFragment.this.getActivity());
//				} else {
//					Log.d("PEN", "Pro not consumed");
//				}
//				
//				iabHelper.dispose();
//			}
//		};
//		
//		final IabHelper.QueryInventoryFinishedListener queryListener = new IabHelper.QueryInventoryFinishedListener() {
//			@Override
//			public void onQueryInventoryFinished(IabResult result, Inventory inv) {
//				Log.d("PEN", "query for consumption done");
//				if (result.isSuccess()) {
//					Log.d("PEN", "query for consumption successful");
//					iabHelper.consumeAsync(inv.getPurchase(FreehandIabHelper.SKU_PRO), consumeListener);
//				}
//			}
//		};
//		
//		final IabHelper.OnIabSetupFinishedListener setupListener = new IabHelper.OnIabSetupFinishedListener() {
//			@Override
//			public void onIabSetupFinished(IabResult result) {
//				Log.d("PEN", "setup for consumption done");
//				iabHelper.queryInventoryAsync(queryListener);
//			}
//		};
//		
//		iabHelper.startSetup(setupListener);
//    }
    
    @Override
    public void onActivityResult (final int requestCode, final int resultCode, final Intent data) {
    	if (!FreehandIabHelper.handleActivityResult(requestCode, resultCode, data)) {
    		super.onActivityResult(requestCode, resultCode, data);
    	}
    }
}