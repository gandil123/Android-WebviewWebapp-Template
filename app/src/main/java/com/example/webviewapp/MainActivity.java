package com.qandilalzman.digital;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.onesignal.Continue;
import com.onesignal.OneSignal;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

private WebView webView;
private WebView splashWebView;

private ValueCallback<Uri[]> uploadMessage;
private Uri cameraUri;

private static final int REQUEST_CODE_FILE = 100;
private static final int REQUEST_PERMISSION_CAMERA = 200;

private static final String WEBSITE_URL =
        "https://qandilalzman-from.cc.cd/";

private static final String MAIN_DOMAIN =
        "qandilalzman-from.cc.cd";

private static final String SPLASH_URL =
        "file:///android_asset/splash.html";

private static final String OFFLINE_URL =
        "file:///android_asset/offline.html";

private static final long SPLASH_DURATION = 2500;

private boolean showingSplash = false;
private boolean showingOfflinePage = false;
private boolean returningOnline = false;
private boolean notificationDialogShown = false;

private ConnectivityManager connectivityManager;
private ConnectivityManager.NetworkCallback networkCallback;


@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    webView = findViewById(R.id.webView);
    splashWebView = findViewById(R.id.splashWebView);

    setupWebView();
    setupSplash();

    if (isNetworkAvailable()) {
        loadWebsite();
    } else {
        showOfflinePage();
    }

    showSplash();

    registerNetworkCallback();
}


private void setupSplash() {

    if (splashWebView == null) {
        return;
    }

    WebSettings settings =
            splashWebView.getSettings();

    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(true);
    settings.setAllowContentAccess(true);

    splashWebView.setWebViewClient(
            new WebViewClient()
    );

    splashWebView.loadUrl(SPLASH_URL);
}


private void showSplash() {

    showingSplash = true;

    if (splashWebView == null) {
        return;
    }

    splashWebView.setVisibility(View.VISIBLE);
    splashWebView.bringToFront();

    splashWebView.postDelayed(
            new Runnable() {
                @Override
                public void run() {
                    hideSplash();
                }
            },
            SPLASH_DURATION
    );
}


private void hideSplash() {

    showingSplash = false;

    if (splashWebView != null) {
        splashWebView.setVisibility(View.GONE);
    }

    if (webView != null) {
        webView.bringToFront();
    }

    showNotificationDialogIfNeeded();
}


/*
 * نافذة تفعيل الإشعارات
 */
private void showNotificationDialogIfNeeded() {

    if (notificationDialogShown) {
        return;
    }

    /*
     * لا تظهر النافذة إذا كان الإذن ممنوحًا بالفعل.
     */
    try {
        if (OneSignal.getNotifications().getPermission()) {
            notificationDialogShown = true;
            return;
        }
    } catch (Exception ignored) {
    }

    notificationDialogShown = true;

    new AlertDialog.Builder(this)
            .setTitle("تفعيل الإشعارات")
            .setMessage(
                    "هل تريد تفعيل إشعارات قنديل الزمان للحصول على التنبيهات والتحديثات؟"
            )
            .setPositiveButton(
                    "تفعيل",
                    (dialog, which) -> {
                        requestNotificationPermission();
                    }
            )
            .setNegativeButton(
                    "لاحقًا",
                    null
            )
            .show();
}


/*
 * طلب إذن الإشعارات عن طريق OneSignal
 *
 * Continue.none() هو الأسلوب الصحيح
 * عند استخدام OneSignal SDK 5.x في Java.
 */
private void requestNotificationPermission() {

    try {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    "android.permission.POST_NOTIFICATIONS"
            ) != PackageManager.PERMISSION_GRANTED) {

                /*
                 * OneSignal هو المسؤول عن فتح
                 * نافذة إذن الإشعارات.
                 */
                OneSignal.getNotifications()
                        .requestPermission(
                                false,
                                Continue.none()
                        );

                return;
            }
        }

        /*
         * للأجهزة التي لا تحتاج إذن Android 13.
         */
        OneSignal.getNotifications()
                .requestPermission(
                        false,
                        Continue.none()
                );

    } catch (Exception e) {

        Toast.makeText(
                this,
                "تعذر فتح طلب الإشعارات.",
                Toast.LENGTH_LONG
        ).show();
    }
}


/*
 * إعداد WebView
 */
private void setupWebView() {

    WebSettings webSettings =
            webView.getSettings();

    webSettings.setJavaScriptEnabled(true);
    webSettings.setDomStorageEnabled(true);

    webSettings.setAllowFileAccess(true);
    webSettings.setAllowContentAccess(true);

    webSettings.setDatabaseEnabled(true);

    webSettings.setLoadWithOverviewMode(true);
    webSettings.setUseWideViewPort(true);

    webSettings.setSupportZoom(true);
    webSettings.setBuiltInZoomControls(true);
    webSettings.setDisplayZoomControls(false);

    webSettings.setCacheMode(
            WebSettings.LOAD_DEFAULT
    );

    if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.LOLLIPOP) {

        webSettings.setMixedContentMode(
                WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        );
    }

    webSettings.setSaveFormData(true);
    webSettings.setGeolocationEnabled(true);

    CookieManager cookieManager =
            CookieManager.getInstance();

    cookieManager.setAcceptCookie(true);

    if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.LOLLIPOP) {

        cookieManager.setAcceptThirdPartyCookies(
                webView,
                true
        );
    }


    webView.setWebViewClient(
            new WebViewClient() {

                @Override
                public boolean shouldOverrideUrlLoading(
                        WebView view,
                        WebResourceRequest request
                ) {

                    if (Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.LOLLIPOP) {

                        return handleUrl(
                                request.getUrl()
                        );
                    }

                    return false;
                }


                @Override
                public boolean shouldOverrideUrlLoading(
                        WebView view,
                        String url
                ) {

                    return handleUrl(
                            Uri.parse(url)
                    );
                }


                @Override
                public void onPageFinished(
                        WebView view,
                        String url
                ) {

                    super.onPageFinished(
                            view,
                            url
                    );

                    if (url != null
                            && !url.equals(
                            OFFLINE_URL
                    )) {

                        showingOfflinePage = false;

                        CookieManager
                                .getInstance()
                                .flush();
                    }
                }


                @Override
                public void onReceivedError(
                        WebView view,
                        WebResourceRequest request,
                        WebResourceError error
                ) {

                    super.onReceivedError(
                            view,
                            request,
                            error
                    );

                    if (Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.M) {

                        if (request.isForMainFrame()) {
                            showOfflinePage();
                        }

                    } else {

                        showOfflinePage();
                    }
                }


                @Override
                public void onReceivedError(
                        WebView view,
                        int errorCode,
                        String description,
                        String failingUrl
                ) {

                    super.onReceivedError(
                            view,
                            errorCode,
                            description,
                            failingUrl
                    );

                    if (!showingSplash) {
                        showOfflinePage();
                    }
                }
            }
    );


    webView.setWebChromeClient(
            new WebChromeClient() {

                @Override
                public boolean onShowFileChooser(
                        WebView webView,
                        ValueCallback<Uri[]> filePathCallback,
                        FileChooserParams fileChooserParams
                ) {

                    if (uploadMessage != null) {

                        uploadMessage.onReceiveValue(null);
                        uploadMessage = null;
                    }

                    uploadMessage = filePathCallback;

                    boolean canCapture =
                            fileChooserParams
                                    .isCaptureEnabled();


                    Intent fileIntent =
                            new Intent(
                                    Intent.ACTION_GET_CONTENT
                            );

                    fileIntent.addCategory(
                            Intent.CATEGORY_OPENABLE
                    );

                    fileIntent.setType("*/*");

                    fileIntent.putExtra(
                            Intent.EXTRA_ALLOW_MULTIPLE,
                            true
                    );


                    if (canCapture) {

                        if (checkCameraPermission()) {

                            Intent cameraIntent =
                                    createCameraIntent();

                            if (cameraIntent != null) {

                                Intent chooser =
                                        Intent.createChooser(
                                                fileIntent,
                                                "اختيار ملف أو صورة"
                                        );

                                chooser.putExtra(
                                        Intent.EXTRA_INITIAL_INTENTS,
                                        new Intent[]{
                                                cameraIntent
                                        }
                                );

                                try {

                                    startActivityForResult(
                                            chooser,
                                            REQUEST_CODE_FILE
                                    );

                                    return true;

                                } catch (
                                        ActivityNotFoundException e
                                ) {
                                }
                            }

                        } else {

                            ActivityCompat.requestPermissions(
                                    MainActivity.this,
                                    new String[]{
                                            "android.permission.CAMERA"
                                    },
                                    REQUEST_PERMISSION_CAMERA
                            );

                            return true;
                        }
                    }


                    try {

                        startActivityForResult(
                                fileIntent,
                                REQUEST_CODE_FILE
                        );

                    } catch (
                            ActivityNotFoundException e
                    ) {

                        if (uploadMessage != null) {

                            uploadMessage.onReceiveValue(null);
                            uploadMessage = null;
                        }

                        Toast.makeText(
                                MainActivity.this,
                                "لا يوجد تطبيق لاختيار الملفات.",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    return true;
                }
            }
    );
}


private boolean isNetworkAvailable() {

    ConnectivityManager cm =
            (ConnectivityManager)
                    getSystemService(
                            Context.CONNECTIVITY_SERVICE
                    );

    if (cm == null) {
        return false;
    }

    if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.M) {

        Network network =
                cm.getActiveNetwork();

        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities =
                cm.getNetworkCapabilities(
                        network
                );

        if (capabilities == null) {
            return false;
        }

        return capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
        );
    }

    android.net.NetworkInfo networkInfo =
            cm.getActiveNetworkInfo();

    return networkInfo != null
            && networkInfo.isConnected();
}


private void loadWebsite() {

    if (webView == null) {
        return;
    }

    showingOfflinePage = false;

    webView.loadUrl(WEBSITE_URL);
}


private void showOfflinePage() {

    if (webView == null) {
        return;
    }

    if (showingOfflinePage) {
        return;
    }

    showingOfflinePage = true;

    webView.loadUrl(OFFLINE_URL);
}


private void registerNetworkCallback() {

    if (Build.VERSION.SDK_INT <
            Build.VERSION_CODES.N) {

        return;
    }

    connectivityManager =
            (ConnectivityManager)
                    getSystemService(
                            Context.CONNECTIVITY_SERVICE
                    );

    if (connectivityManager == null) {
        return;
    }

    networkCallback =
            new ConnectivityManager.NetworkCallback() {

                @Override
                public void onAvailable(
                        Network network
                ) {

                    runOnUiThread(
                            new Runnable() {

                                @Override
                                public void run() {
                                    returnToWebsite();
                                }
                            }
                    );
                }


                @Override
                public void onLost(
                        Network network
                ) {

                    runOnUiThread(
                            new Runnable() {

                                @Override
                                public void run() {

                                    if (!showingSplash) {
                                        showOfflinePage();
                                    }
                                }
                            }
                    );
                }
            };


    try {

        connectivityManager
                .registerDefaultNetworkCallback(
                        networkCallback
                );

    } catch (Exception ignored) {
    }
}


private void returnToWebsite() {

    if (webView == null) {
        return;
    }

    if (showingSplash) {
        return;
    }

    if (returningOnline) {
        return;
    }

    String currentUrl =
            webView.getUrl();

    if (currentUrl != null
            && currentUrl.equals(
            OFFLINE_URL
    )) {

        returningOnline = true;

        showingOfflinePage = false;

        webView.loadUrl(WEBSITE_URL);

        webView.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {
                        returningOnline = false;
                    }

                },
                1500
        );
    }
}


private boolean handleUrl(Uri uri) {

    if (uri == null) {
        return true;
    }

    String scheme =
            uri.getScheme();

    if (scheme == null) {
        return false;
    }


    if (scheme.equalsIgnoreCase("http")
            || scheme.equalsIgnoreCase("https")) {

        String host =
                uri.getHost();

        if (host != null
                && (
                host.equalsIgnoreCase(
                        MAIN_DOMAIN
                )
                        || host.endsWith(
                        "." + MAIN_DOMAIN
                )
        )) {

            return false;
        }


        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            uri
                    );

            startActivity(intent);

        } catch (
                ActivityNotFoundException e
        ) {

            Toast.makeText(
                    MainActivity.this,
                    "لا يمكن فتح هذا الرابط.",
                    Toast.LENGTH_SHORT
            ).show();
        }

        return true;
    }


    if (scheme.equalsIgnoreCase("tel")
            || scheme.equalsIgnoreCase("mailto")
            || scheme.equalsIgnoreCase("sms")
            || scheme.equalsIgnoreCase("geo")) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            uri
                    );

            startActivity(intent);

        } catch (
                ActivityNotFoundException e
        ) {

            Toast.makeText(
                    MainActivity.this,
                    "لا يوجد تطبيق مناسب لفتح هذا الرابط.",
                    Toast.LENGTH_SHORT
            ).show();
        }

        return true;
    }


    try {

        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW,
                        uri
                );

        startActivity(intent);

    } catch (
            ActivityNotFoundException ignored
    ) {
    }

    return true;
}


private Intent createCameraIntent() {

    Intent intent =
            new Intent(
                    MediaStore.ACTION_IMAGE_CAPTURE
            );

    if (intent.resolveActivity(
            getPackageManager()
    ) == null) {

        return null;
    }

    try {

        File photoFile =
                createImageFile();

        if (photoFile == null) {
            return null;
        }

        cameraUri =
                FileProvider.getUriForFile(
                        this,
                        getPackageName()
                                + ".fileprovider",
                        photoFile
                );

        intent.putExtra(
                MediaStore.EXTRA_OUTPUT,
                cameraUri
        );

        intent.addFlags(
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_READ_URI_PERMISSION
        );

        return intent;

    } catch (Exception e) {

        Toast.makeText(
                this,
                "تعذر تجهيز الكاميرا.",
                Toast.LENGTH_SHORT
        ).show();

        return null;
    }
}


private File createImageFile()
        throws IOException {

    String timeStamp =
            new SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.getDefault()
            ).format(new Date());

    String imageFileName =
            "Qandil_" + timeStamp + "_";

    File storageDir =
            getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES
            );

    return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
    );
}


private boolean checkCameraPermission() {

    if (Build.VERSION.SDK_INT <
            Build.VERSION_CODES.M) {

        return true;
    }

    return ContextCompat.checkSelfPermission(
            this,
            "android.permission.CAMERA"
    ) == PackageManager.PERMISSION_GRANTED;
}


@Override
protected void onActivityResult(
        int requestCode,
        int resultCode,
        @Nullable Intent data
) {

    super.onActivityResult(
            requestCode,
            resultCode,
            data
    );

    if (requestCode != REQUEST_CODE_FILE) {
        return;
    }

    if (uploadMessage == null) {
        return;
    }

    Uri[] results = null;


    if (resultCode != Activity.RESULT_OK) {

        uploadMessage.onReceiveValue(null);

        uploadMessage = null;
        cameraUri = null;

        return;
    }


    if (data == null) {

        if (cameraUri != null) {

            results =
                    new Uri[]{
                            cameraUri
                    };
        }

    } else {

        if (data.getClipData() != null) {

            int count =
                    data.getClipData()
                            .getItemCount();

            results =
                    new Uri[count];

            for (int i = 0; i < count; i++) {

                results[i] =
                        data.getClipData()
                                .getItemAt(i)
                                .getUri();
            }

        } else if (data.getData() != null) {

            results =
                    new Uri[]{
                            data.getData()
                    };
        }
    }

    uploadMessage.onReceiveValue(results);

    uploadMessage = null;
    cameraUri = null;
}


@Override
public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
) {

    super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
    );

    if (requestCode ==
            REQUEST_PERMISSION_CAMERA) {

        if (grantResults.length > 0
                && grantResults[0] ==
                PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(
                    this,
                    "تم السماح بالكاميرا. اضغط رفع الملف مرة أخرى.",
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            Toast.makeText(
                    this,
                    "لم يتم السماح باستخدام الكاميرا.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}


@Override
public void onBackPressed() {

    if (webView == null) {

        showExitDialog();
        return;
    }

    String currentUrl =
            webView.getUrl();

    if (currentUrl != null
            && currentUrl.equals(
            OFFLINE_URL
    )) {

        showExitDialog();
        return;
    }

    if (webView.canGoBack()) {

        webView.goBack();
        return;
    }

    showExitDialog();
}


private void showExitDialog() {

    new AlertDialog.Builder(this)

            .setTitle("تأكيد الخروج")

            .setMessage(
                    "هل أنت متأكد من الخروج من التطبيق؟"
            )

            .setPositiveButton(
                    "موافق",
                    (dialog, which) -> {
                        finish();
                    }
            )

            .setNegativeButton(
                    "إلغاء",
                    null
            )

            .show();
}


@Override
protected void onSaveInstanceState(
        Bundle outState
) {

    if (webView != null) {

        webView.saveState(
                outState
        );
    }

    super.onSaveInstanceState(
            outState
    );
}


@Override
protected void onDestroy() {

    if (connectivityManager != null
            && networkCallback != null
            && Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.N) {

        try {

            connectivityManager
                    .unregisterNetworkCallback(
                            networkCallback
                    );

        } catch (Exception ignored) {
        }

        networkCallback = null;
    }


    if (splashWebView != null) {

        splashWebView.stopLoading();
        splashWebView.setWebChromeClient(null);
        splashWebView.setWebViewClient(null);
        splashWebView.destroy();

        splashWebView = null;
    }


    if (webView != null) {

        webView.stopLoading();
        webView.setWebChromeClient(null);
        webView.setWebViewClient(null);
        webView.destroy();

        webView = null;
    }

    super.onDestroy();
}

}
