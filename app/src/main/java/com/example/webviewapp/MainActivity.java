package com.qandilalzman.digital;

import android.Manifest;
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
    private static final int REQUEST_PERMISSION_NOTIFICATIONS = 300;

    /*
     * رابط الموقع
     */
    private static final String WEBSITE_URL =
            "https://qandilalzman-from.cc.cd/";

    /*
     * نطاق الموقع
     */
    private static final String MAIN_DOMAIN =
            "qandilalzman-from.cc.cd";

    /*
     * شاشة البداية
     */
    private static final String SPLASH_URL =
            "file:///android_asset/splash.html";

    /*
     * صفحة عدم الاتصال
     */
    private static final String OFFLINE_URL =
            "file:///android_asset/offline.html";

    /*
     * مدة شاشة البداية
     */
    private static final long SPLASH_DURATION = 2500;

    /*
     * حالة شاشة البداية
     */
    private boolean showingSplash = false;

    /*
     * حالة Offline
     */
    private boolean showingOfflinePage = false;

    /*
     * منع تكرار العودة للموقع
     */
    private boolean returningOnline = false;

    /*
     * منع تكرار نافذة الإشعارات
     */
    private boolean notificationDialogShown = false;

    /*
     * مراقبة الشبكة
     */
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

        /*
         * تحميل الموقع أو Offline
         */
        if (isNetworkAvailable()) {
            loadWebsite();
        } else {
            showOfflinePage();
        }

        /*
         * إظهار Splash
         */
        showSplash();

        /*
         * مراقبة الإنترنت
         */
        registerNetworkCallback();
    }


    /*
     * إعداد شاشة البداية
     */
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


    /*
     * عرض Splash
     */
    private void showSplash() {

        showingSplash = true;

        if (splashWebView == null) {
            return;
        }

        splashWebView.setVisibility(
                View.VISIBLE
        );

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


    /*
     * إخفاء Splash
     */
    private void hideSplash() {

        showingSplash = false;

        if (splashWebView != null) {

            splashWebView.setVisibility(
                    View.GONE
            );
        }

        if (webView != null) {

            webView.bringToFront();
        }

        /*
         * عرض خيار الإشعارات بعد Splash
         */
        showNotificationDialogIfNeeded();
    }


    /*
     * سؤال المستخدم عن تفعيل الإشعارات
     *
     * لا يتم تشغيل الإشعارات تلقائيًا.
     * المستخدم هو من يختار.
     */
    private void showNotificationDialogIfNeeded() {

        if (notificationDialogShown) {
            return;
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
     * طلب إذن الإشعارات
     */
    private void requestNotificationPermission() {

        /*
         * Android 13 وما بعده
         */
        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        REQUEST_PERMISSION_NOTIFICATIONS
                );

            } else {

                enableOneSignal();
            }

        } else {

            /*
             * Android 7 إلى Android 12
             */
            enableOneSignal();
        }
    }


    /*
     * تفعيل إشعارات OneSignal
     */
    private void enableOneSignal() {

        OneSignal.getNotifications()
                .requestPermission(false, null);
    }


    /*
     * إعداد WebView الموقع
     */
    private void setupWebView() {

        WebSettings webSettings =
                webView.getSettings();

        /*
         * JavaScript
         */
        webSettings.setJavaScriptEnabled(true);

        /*
         * Local Storage
         */
        webSettings.setDomStorageEnabled(true);

        /*
         * الملفات
         */
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        /*
         * Database
         */
        webSettings.setDatabaseEnabled(true);

        /*
         * تحسين عرض الموقع
         */
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        /*
         * التكبير
         */
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        /*
         * Cache
         */
        webSettings.setCacheMode(
                WebSettings.LOAD_DEFAULT
        );

        /*
         * Mixed Content
         */
        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.LOLLIPOP) {

            webSettings.setMixedContentMode(
                    WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            );
        }

        /*
         * حفظ بيانات النماذج
         */
        webSettings.setSaveFormData(true);

        /*
         * الموقع الجغرافي
         */
        webSettings.setGeolocationEnabled(true);

        /*
         * Cookies
         */
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


        /*
         * WebViewClient
         */
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


                    /*
                     * انتهاء تحميل الموقع
                     */
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


                    /*
                     * خطأ تحميل الصفحة
                     */
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


                    /*
                     * Android القديم
                     */
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


        /*
         * WebChromeClient
         */
        webView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public boolean onShowFileChooser(
                            WebView webView,
                            ValueCallback<Uri[]> filePathCallback,
                            FileChooserParams fileChooserParams
                    ) {

                        /*
                         * إلغاء اختيار سابق
                         */
                        if (uploadMessage != null) {

                            uploadMessage.onReceiveValue(
                                    null
                            );

                            uploadMessage = null;
                        }

                        uploadMessage =
                                filePathCallback;


                        boolean canCapture =
                                fileChooserParams
                                        .isCaptureEnabled();


                        /*
                         * منتقي الملفات
                         */
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


                        /*
                         * الكاميرا
                         */
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
                                                Manifest.permission.CAMERA
                                        },
                                        REQUEST_PERMISSION_CAMERA
                                );

                                return true;
                            }
                        }


                        /*
                         * فتح منتقي الملفات
                         */
                        try {

                            startActivityForResult(
                                    fileIntent,
                                    REQUEST_CODE_FILE
                            );

                        } catch (
                                ActivityNotFoundException e
                        ) {

                            if (uploadMessage != null) {

                                uploadMessage.onReceiveValue(
                                        null
                                );

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


    /*
     * فحص الإنترنت
     */
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


    /*
     * تحميل الموقع
     */
    private void loadWebsite() {

        if (webView == null) {
            return;
        }

        showingOfflinePage = false;

        webView.loadUrl(
                WEBSITE_URL
        );
    }


    /*
     * عرض Offline
     */
    private void showOfflinePage() {

        if (webView == null) {
            return;
        }

        if (showingOfflinePage) {
            return;
        }

        showingOfflinePage = true;

        webView.loadUrl(
                OFFLINE_URL
        );
    }


    /*
     * مراقبة الشبكة
     */
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

                    /*
                     * عودة الإنترنت
                     */
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


                    /*
                     * انقطاع الإنترنت
                     */
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


    /*
     * العودة للموقع عند رجوع الإنترنت
     */
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

            webView.loadUrl(
                    WEBSITE_URL
            );


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


    /*
     * معالجة الروابط
     */
    private boolean handleUrl(Uri uri) {

        if (uri == null) {
            return true;
        }

        String scheme =
                uri.getScheme();

        if (scheme == null) {
            return false;
        }


        /*
         * HTTP / HTTPS
         */
        if (scheme.equalsIgnoreCase("http")
                || scheme.equalsIgnoreCase("https")) {

            String host =
                    uri.getHost();


            /*
             * موقع قنديل الزمان
             * يبقى داخل التطبيق
             */
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


            /*
             * المواقع الخارجية
             */
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


        /*
         * الهاتف والبريد والرسائل والموقع
         */
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


        /*
         * أي Scheme آخر
         */
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


    /*
     * إنشاء Intent الكاميرا
     */
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


    /*
     * إنشاء ملف الصورة
     */
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


    /*
     * فحص إذن الكاميرا
     */
    private boolean checkCameraPermission() {

        if (Build.VERSION.SDK_INT
                < Build.VERSION_CODES.M) {

            return true;
        }


        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }


    /*
     * نتيجة اختيار الملفات
     */
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


        /*
         * إلغاء
         */
        if (resultCode != Activity.RESULT_OK) {

            uploadMessage.onReceiveValue(
                    null
            );

            uploadMessage = null;

            cameraUri = null;

            return;
        }


        /*
         * الكاميرا
         */
        if (data == null) {

            if (cameraUri != null) {

                results =
                        new Uri[]{
                                cameraUri
                        };
            }

        } else {


            /*
             * عدة ملفات
             */
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


            }

            /*
             * ملف واحد
             */
            else if (data.getData() != null) {

                results =
                        new Uri[]{
                                data.getData()
                        };
            }
        }


        uploadMessage.onReceiveValue(
                results
        );

        uploadMessage = null;

        cameraUri = null;
    }


    /*
     * نتائج الصلاحيات
     */
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


        /*
         * صلاحية الكاميرا
         */
        if (requestCode
                == REQUEST_PERMISSION_CAMERA) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

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


        /*
         * صلاحية الإشعارات
         */
        if (requestCode
                == REQUEST_PERMISSION_NOTIFICATIONS) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                enableOneSignal();

                Toast.makeText(
                        this,
                        "تم تفعيل الإشعارات.",
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                Toast.makeText(
                        this,
                        "لم يتم تفعيل الإشعارات.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }


    /*
     * زر الرجوع
     */
    @Override
    public void onBackPressed() {

        if (webView == null) {

            showExitDialog();

            return;
        }


        String currentUrl =
                webView.getUrl();


        /*
         * إذا كانت صفحة Offline
         */
        if (currentUrl != null
                && currentUrl.equals(
                OFFLINE_URL
        )) {

            showExitDialog();

            return;
        }


        /*
         * إذا توجد صفحة سابقة داخل الموقع
         */
        if (webView.canGoBack()) {

            webView.goBack();

            return;
        }


        /*
         * لا توجد صفحة سابقة
         */
        showExitDialog();
    }


    /*
     * حوار تأكيد الخروج
     */
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


    /*
     * حفظ حالة WebView
     */
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


    /*
     * تنظيف التطبيق
     */
    @Override
    protected void onDestroy() {

        /*
         * إلغاء مراقبة الشبكة
         */
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


        /*
         * تنظيف Splash
         */
        if (splashWebView != null) {

            splashWebView.stopLoading();

            splashWebView.setWebChromeClient(null);

            splashWebView.setWebViewClient(null);

            splashWebView.destroy();

            splashWebView = null;
        }


        /*
         * تنظيف WebView الموقع
         */
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
