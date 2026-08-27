package com.qandilalzman.digital;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
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
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;

    private ValueCallback<Uri[]> uploadMessage;
    private Uri cameraUri;

    private static final int REQUEST_CODE_FILE = 100;
    private static final int REQUEST_PERMISSION_CAMERA = 200;

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
     * ملفات التطبيق المحلية
     */
    private static final String SPLASH_URL =
            "file:///android_asset/splash.html";

    private static final String OFFLINE_URL =
            "file:///android_asset/offline.html";

    /*
     * مدة شاشة البداية
     * 2500 = 2.5 ثانية
     */
    private static final long SPLASH_DURATION = 2500;

    /*
     * لمنع تكرار فتح صفحة عدم الاتصال
     */
    private boolean showingOfflinePage = false;

    /*
     * لمعرفة أن Splash ظاهرة
     */
    private boolean showingSplash = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        setupWebView();

        /*
         * إخفاء شريط التحميل عند بداية التطبيق
         */
        if (progressBar != null) {
            progressBar.setVisibility(ProgressBar.GONE);
        }

        /*
         * عرض Splash المحلية فورًا.
         *
         * لا تحتاج إلى إنترنت إطلاقًا.
         */
        showingSplash = true;
        showingOfflinePage = false;

        webView.loadUrl(SPLASH_URL);

        /*
         * بعد انتهاء Splash:
         *
         * يوجد إنترنت → الموقع
         *
         * لا يوجد إنترنت → offline.html
         */
        webView.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (webView == null) {
                    return;
                }

                showingSplash = false;

                if (isNetworkAvailable()) {

                    loadWebsite();

                } else {

                    showOfflinePage();
                }

            }
        }, SPLASH_DURATION);
    }


    /*
     * إعداد WebView
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
         * الملفات المحلية
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
         *
         * نترك WebView يستخدم الكاش الطبيعي
         * للموقع.
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

                    /*
                     * اعتراض الروابط
                     */
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


                    /*
                     * دعم Android القديم
                     */
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
                     * بداية تحميل الصفحة
                     */
                    @Override
                    public void onPageStarted(
                            WebView view,
                            String url,
                            android.graphics.Bitmap favicon
                    ) {

                        super.onPageStarted(
                                view,
                                url,
                                favicon
                        );

                        /*
                         * صفحات التطبيق المحلية
                         * لا يظهر معها ProgressBar
                         */
                        if (url.startsWith(
                                "file:///android_asset/"
                        )) {

                            if (progressBar != null) {

                                progressBar.setVisibility(
                                        ProgressBar.GONE
                                );
                            }

                            return;
                        }

                        /*
                         * الموقع الحقيقي
                         */
                        if (progressBar != null) {

                            progressBar.setProgress(0);

                            progressBar.setVisibility(
                                    ProgressBar.VISIBLE
                            );
                        }
                    }


                    /*
                     * انتهاء تحميل الصفحة
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

                        /*
                         * الصفحات المحلية
                         */
                        if (url.startsWith(
                                "file:///android_asset/"
                        )) {

                            if (progressBar != null) {

                                progressBar.setVisibility(
                                        ProgressBar.GONE
                                );
                            }

                            return;
                        }

                        /*
                         * الموقع
                         */
                        if (progressBar != null) {

                            progressBar.setProgress(100);

                            progressBar.setVisibility(
                                    ProgressBar.GONE
                            );
                        }

                        /*
                         * تم تحميل الموقع بنجاح
                         */
                        showingOfflinePage = false;

                        CookieManager
                                .getInstance()
                                .flush();
                    }


                    /*
                     * خطأ تحميل الصفحة
                     *
                     * هذا هو الجزء المهم.
                     *
                     * إذا ظهر:
                     * ERR_ADDRESS_UNREACHABLE
                     * ERR_INTERNET_DISCONNECTED
                     * ERR_CONNECTION_FAILED
                     *
                     * نعرض offline.html
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

                        /*
                         * نهتم فقط بالصفحة الرئيسية.
                         *
                         * لا نعرض offline.html بسبب
                         * صورة أو ملف JavaScript فشل.
                         */
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
                     * دعم Android القديم
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

                        /*
                         * لا نعرضها أثناء Splash
                         */
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

                    /*
                     * ProgressBar
                     */
                    @Override
                    public void onProgressChanged(
                            WebView view,
                            int newProgress
                    ) {

                        super.onProgressChanged(
                                view,
                                newProgress
                        );

                        if (progressBar == null) {
                            return;
                        }

                        String currentUrl =
                                view.getUrl();

                        /*
                         * لا تعرض ProgressBar
                         * في Splash أو offline
                         */
                        if (currentUrl != null
                                && currentUrl.startsWith(
                                "file:///android_asset/"
                        )) {

                            progressBar.setVisibility(
                                    ProgressBar.GONE
                            );

                            return;
                        }

                        /*
                         * الموقع الحقيقي
                         */
                        if (newProgress >= 100) {

                            progressBar.setVisibility(
                                    ProgressBar.GONE
                            );

                        } else {

                            progressBar.setProgress(
                                    newProgress
                            );

                            progressBar.setVisibility(
                                    ProgressBar.VISIBLE
                            );
                        }
                    }


                    /*
                     * اختيار الملفات والكاميرا
                     */
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

                        /*
                         * هل الموقع طلب الكاميرا؟
                         */
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
                                        // نكمل إلى منتقي الملفات
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
     * فحص حالة اتصال الهاتف
     */
    private boolean isNetworkAvailable() {

        ConnectivityManager connectivityManager =
                (ConnectivityManager)
                        getSystemService(
                                CONNECTIVITY_SERVICE
                        );

        if (connectivityManager == null) {
            return false;
        }

        /*
         * Android 6 وما بعده
         */
        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M) {

            Network network =
                    connectivityManager
                            .getActiveNetwork();

            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities =
                    connectivityManager
                            .getNetworkCapabilities(
                                    network
                            );

            if (capabilities == null) {
                return false;
            }

            return capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
            );
        }

        /*
         * Android القديم
         */
        android.net.NetworkInfo networkInfo =
                connectivityManager
                        .getActiveNetworkInfo();

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

        if (progressBar != null) {

            progressBar.setProgress(0);

            progressBar.setVisibility(
                    ProgressBar.VISIBLE
            );
        }

        webView.loadUrl(
                WEBSITE_URL
        );
    }


    /*
     * عرض صفحة عدم الاتصال
     */
    private void showOfflinePage() {

        if (webView == null) {
            return;
        }

        /*
         * منع تكرار تحميل الصفحة
         */
        if (showingOfflinePage) {
            return;
        }

        showingOfflinePage = true;

        if (progressBar != null) {

            progressBar.setVisibility(
                    ProgressBar.GONE
            );
        }

        /*
         * هذه الصفحة موجودة داخل APK
         * ولا تحتاج إلى الإنترنت.
         */
        webView.loadUrl(
                OFFLINE_URL
        );
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
             * روابط موقع Qandilalzman
             * تبقى داخل WebView
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
             * الروابط الخارجية
             * تفتح خارج التطبيق
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
     * إنشاء Intent للكاميرا
     */
    private Intent createCameraIntent() {

        Intent intent =
                new Intent(
                        MediaStore.ACTION_IMAGE_CAPTURE
                );

        /*
         * التأكد من وجود تطبيق كاميرا
         */
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

            /*
             * إنشاء URI آمن للكاميرا
             */
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
     * إنشاء ملف الصورة المؤقت
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
     * نتيجة اختيار الملفات أو الكاميرا
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

        /*
         * لسنا مهتمين بأي Activity أخرى
         */
        if (requestCode != REQUEST_CODE_FILE) {
            return;
        }

        /*
         * لا يوجد Callback
         */
        if (uploadMessage == null) {
            return;
        }

        Uri[] results = null;


        /*
         * إلغاء الاختيار
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
         * الصورة القادمة من الكاميرا
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
             * أكثر من ملف
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


        /*
         * إرسال النتيجة إلى WebView
         */
        uploadMessage.onReceiveValue(
                results
        );

        uploadMessage = null;
        cameraUri = null;
    }


    /*
     * نتيجة طلب صلاحية الكاميرا
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
    }


    /*
     * زر الرجوع
     */
    @Override
    public void onBackPressed() {

        if (webView != null
                && webView.canGoBack()) {

            String currentUrl =
                    webView.getUrl();

            /*
             * إذا كان المستخدم في Splash
             * أو offline لا نرجع لصفحة
             * الإنترنت السابقة.
             */
            if (currentUrl != null
                    && (
                    currentUrl.equals(SPLASH_URL)
                            || currentUrl.equals(OFFLINE_URL)
            )) {

                super.onBackPressed();

                return;
            }

            webView.goBack();

        } else {

            super.onBackPressed();
        }
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
     * تنظيف WebView
     */
    @Override
    protected void onDestroy() {

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
