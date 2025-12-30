package com.devhjs.mathgraphstudy.util
 
 import android.app.Activity
 import android.content.Context
 import android.util.Log
 import com.google.android.gms.ads.AdRequest
 import com.google.android.gms.ads.LoadAdError
 import com.google.android.gms.ads.MobileAds
 import com.google.android.gms.ads.interstitial.InterstitialAd
 import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
 
 object AdManager {
     private const val AD_UNIT_ID = "ca-app-pub-3216980827282944/1022276279"
     private var interstitialAd: InterstitialAd? = null
     private var isAdLoading = false
 
     fun initialize(context: Context) {
         MobileAds.initialize(context) { }
         loadInterstitial(context)
     }
 
     fun loadInterstitial(context: Context) {
         if (interstitialAd != null || isAdLoading) return
 
         isAdLoading = true
         val adRequest = AdRequest.Builder().build()
 
         InterstitialAd.load(
             context,
             AD_UNIT_ID,
             adRequest,
             object : InterstitialAdLoadCallback() {
                 override fun onAdFailedToLoad(adError: LoadAdError) {
                     Log.e("AdManager", adError.message)
                     interstitialAd = null
                     isAdLoading = false
                 }
 
                 override fun onAdLoaded(ad: InterstitialAd) {
                     interstitialAd = ad
                     isAdLoading = false
                 }
             }
         )
     }
 
     fun showInterstitial(activity: Activity) {
         if (interstitialAd != null) {
             interstitialAd?.show(activity)
             interstitialAd = null // Ad shown, reset
             loadInterstitial(activity) // Preload next ad
         } else {
             Log.d("AdManager", "The interstitial ad wasn't ready yet.")
             loadInterstitial(activity)
         }
     }
 }
