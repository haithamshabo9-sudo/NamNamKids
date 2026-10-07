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
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> uploadMessage;
    private final static int FILE_CHOOSER_RESULT_CODE = 1;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private FrameLayout frameLayout;
    private Button backButton;
    private FrameLayout splashScreen; // متغير الشاشة الترحيبية

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. منع إطفاء الشاشة أثناء استخدام التطبيق
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        frameLayout = new FrameLayout(this);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // 2. تحسين الأداء (تفعيل التخزين المؤقت الكاش)
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 3. منع التكبير والتصغير وإخفاء أشرطة التمرير
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // 4. إعداد الشاشة الترحيبية (Splash Screen) برمجياً
        splashScreen = new FrameLayout(this);
        splashScreen.setBackgroundColor(Color.parseColor("#87CEEB")); // لون أزرق سماوي لطيف للأطفال
        
        TextView splashText = new TextView(this);
        splashText.setText("شغف كيدز ترحب بكم 🚀");
        splashText.setTextSize(28);
        splashText.setTextColor(Color.WHITE);
        splashText.setGravity(Gravity.CENTER);
        
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, 
                FrameLayout.LayoutParams.WRAP_CONTENT);
        textParams.gravity = Gravity.CENTER;
        splashScreen.addView(splashText, textParams);

        // 5. حماية الطفل (منع الروابط) وشاشة الخطأ
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // منع فتح مواقع الإنترنت الخارجية لحماية الطفل وإبقائه داخل التطبيق
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return true; 
                }
                return false; 
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // إخفاء الشاشة الترحيبية بلمسة حركية عند اكتمال تحميل اللعبة
                if (splashScreen.getVisibility() == View.VISIBLE) {
                    splashScreen.animate().alpha(0f).setDuration(500).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            splashScreen.setVisibility(View.GONE);
                        }
                    });
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    String errorHtml = "<html><body style='display:flex;justify-content:center;align-items:center;height:100vh;background-color:#f8f9fa;'><h1 style='color:#dc3545;font-family:sans-serif;'>عذراً، حدث خطأ في التحميل! 🦖</h1></body></html>";
                    view.loadData(errorHtml, "text/html", "UTF-8");
                }
            }
        });

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

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;

                webView.setVisibility(View.GONE);
                if (backButton != null) backButton.setVisibility(View.GONE);

                frameLayout.addView(customView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, 
                        FrameLayout.LayoutParams.MATCH_PARENT));
                
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN | 
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | 
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) {
                    return;
                }
                customView.setVisibility(View.GONE);
                frameLayout.removeView(customView);
                customView = null;

                webView.setVisibility(View.VISIBLE);
                if (backButton != null) backButton.setVisibility(View.VISIBLE);

                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }
                
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        });

        webView.loadUrl("file:///android_asset/index.html");

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                    request.setTitle("Shaghaf Kids");
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

        // ترتيب الواجهات: وضعنا الويب فيو أولاً، ثم زر الرجوع، ثم الشاشة الترحيبية فوقهم جميعاً
        frameLayout.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 
                FrameLayout.LayoutParams.MATCH_PARENT));

        backButton = new Button(this);
        backButton.setText("🌙"); 
        backButton.setTextSize(32); 
        backButton.setBackgroundColor(Color.TRANSPARENT); 
        backButton.setPadding(15, 15, 15, 15);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, 
                FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.RIGHT; 
        params.setMargins(0, 0, 40, 120); 
        backButton.setLayoutParams(params);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView != null) {
                    webView.loadUrl("file:///android_asset/index.html");
                }
            }
        });

        frameLayout.addView(backButton);
        
        // إضافة الشاشة الترحيبية في النهاية لتكون في الطبقة العليا
        frameLayout.addView(splashScreen, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 
                FrameLayout.LayoutParams.MATCH_PARENT));
                
        setContentView(frameLayout);
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            WebChromeClient client = webView.getWebChromeClient();
            if (client != null) {
                client.onHideCustomView();
            }
        } 
        else if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

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
