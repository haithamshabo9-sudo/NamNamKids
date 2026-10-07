package com.namnam.kids;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> uploadMessage;
    private final static int FILE_CHOOSER_RESULT_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // إنشاء حاوية رئيسية
        FrameLayout frameLayout = new FrameLayout(this);

        // إعداد الـ WebView
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        
        webView.setWebViewClient(new WebViewClient());

        // معالجة اختيار الملفات
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }
                uploadMessage = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE);
                } catch (Exception e) {
                    uploadMessage = null;
                    return false;
                }
                return true;
            }
        });

        // تحميل صفحة الويب
        webView.loadUrl("file:///android_asset/index.html");

        // معالجة التحميلات
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                    request.setTitle("NamNam Kids");
                    request.setDescription("جاري تنزيل الملف...");
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    
                    String fileName = "downloaded_file";
                    if (url != null && url.contains("/")) {
                        fileName = url.substring(url.lastIndexOf('/') + 1);
                        if (fileName.isEmpty()) {
                            fileName = "media_file";
                        }
                    }
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                    DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (manager != null) {
                        manager.enqueue(request);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        frameLayout.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 
                FrameLayout.LayoutParams.MATCH_PARENT));

        // تصميم زر الرجوع
        Button backButton = new Button(this);
        backButton.setText("⬅ رجوع");
        backButton.setTextColor(Color.WHITE);
        backButton.setBackgroundColor(Color.parseColor("#CCFF5722"));
        backButton.setPadding(40, 20, 40, 20);

        // وضع الزر في أسفل الشاشة بالمنتصف
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, 
                FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.setMargins(0, 0, 0, 60);
        backButton.setLayoutParams(params);

        // برمجة وظيفة الرجوع وإغلاق الفيديو
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView != null) {
                    webView.evaluateJavascript(
                        "(function() {" +
                        "  var videos = document.querySelectorAll('video');" +
                        "  videos.forEach(function(v) { v.pause(); v.currentTime = 0; });" +
                        "  if (typeof closeVideo === 'function') { closeVideo(); }" +
                        "  else if (typeof goBack === 'function') { goBack(); }" +
                        "  else {" +
                        "    var videoContainers = document.querySelectorAll('.video-container, .player, .modal, #videoView, #playerView');" +
                        "    videoContainers.forEach(function(el) { el.style.display = 'none'; });" +
                        "    window.history.back();" +
                        "  }" +
                        "})();", null);
                }
            }
        });

        frameLayout.addView(backButton);
        setContentView(frameLayout);
    }

    // تفعيل زر الرجوع الفيزيائي في الهاتف
    @Override
    public void onBackPressed() {
        if (webView != null) {
            webView.evaluateJavascript(
                "(function() {" +
                "  var videos = document.querySelectorAll('video');" +
                "  videos.forEach(function(v) { v.pause(); });" +
                "})();", null);
        }
        super.onBackPressed();
    }

    // استقبال الملفات من الهاتف
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (uploadMessage == null) return;
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && intent != null) {
                String dataString = intent.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                } else if (intent.getClipData() != null) {
                    int count = intent.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = intent.getClipData().getItemAt(i).getUri();
                    }
                }
            }
            uploadMessage.onReceiveValue(results);
            uploadMessage = null;
        }
    }
}
