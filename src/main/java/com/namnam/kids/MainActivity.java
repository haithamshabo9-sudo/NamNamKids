package com.namnam.kids;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** غلاف بسيط: يحمّل index.html من داخل التطبيق عبر https://appassets.androidplatform.net
 *  فيعمل IndexedDB (حفظ الفيديوهات) بشكل دائم داخل بيانات التطبيق ولا يمسحه المتصفح. */
public class MainActivity extends Activity {

    private static final String HOST = "appassets.androidplatform.net";
    private static final int REQ_PICK = 11;
    private static final int REQ_CAPTURE = 12;

    private WebView web;
    private ValueCallback<Uri[]> chooserCb;
    private Uri captureUri;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        clearCaptureCache();

        web = new WebView(this);
        web.setBackgroundColor(0xFF3A1D78);
        setContentView(web);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.addJavascriptInterface(new Bridge(), "AndroidApp");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // لا نسمح بالتنقل خارج التطبيق (أمان للأطفال)
                return !HOST.equals(request.getUrl().getHost());
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (chooserCb != null) chooserCb.onReceiveValue(null);
                chooserCb = callback;
                try {
                    if (params.isCaptureEnabled()) {
                        String[] acc = params.getAcceptTypes();
                        boolean video = acc != null && acc.length > 0 && acc[0] != null
                                && acc[0].startsWith("video");
                        File dir = new File(getCacheDir(), "captures");
                        dir.mkdirs();
                        File f = File.createTempFile(video ? "vid_" : "img_", video ? ".mp4" : ".jpg", dir);
                        captureUri = FileProvider.getUriForFile(
                                MainActivity.this, getPackageName() + ".fileprovider", f);
                        Intent i = new Intent(video ? MediaStore.ACTION_VIDEO_CAPTURE
                                : MediaStore.ACTION_IMAGE_CAPTURE);
                        i.putExtra(MediaStore.EXTRA_OUTPUT, captureUri);
                        i.setClipData(ClipData.newRawUri("", captureUri));
                        i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivityForResult(i, REQ_CAPTURE);
                    } else {
                        startActivityForResult(params.createIntent(), REQ_PICK);
                    }
                } catch (Exception e) {
                    chooserCb = null;
                    captureUri = null;
                    callback.onReceiveValue(null);
                    Toast.makeText(MainActivity.this, "تعذّر فتح الاختيار", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        web.loadUrl("https://" + HOST + "/assets/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK && requestCode != REQ_CAPTURE) return;
        if (chooserCb == null) return;
        Uri[] result = null;
        if (resultCode == RESULT_OK) {
            if (requestCode == REQ_PICK) {
                result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            } else if (captureUri != null) {
                result = new Uri[]{captureUri};
            }
        }
        chooserCb.onReceiveValue(result);
        chooserCb = null;
        captureUri = null;
    }

    /** زر الرجوع: يغلق النوافذ داخل الصفحة، والطفل لا يخرج أثناء المشاهدة. */
    @Override
    public void onBackPressed() {
        String js = "(function(){try{var o=function(i){var e=document.getElementById(i);"
                + "return !!e&&!e.classList.contains('hide')};"
                + "var any=['dlg','pvbox','fabmenu','mathbox','chgbox','pinbox','admin','player'].some(o);"
                + "if(any&&window.__onBack)window.__onBack();return any}catch(e){return false}})()";
        web.evaluateJavascript(js, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (!"true".equals(value)) moveTaskToBack(true);
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    private void clearCaptureCache() {
        try {
            File dir = new File(getCacheDir(), "captures");
            File[] files = dir.listFiles();
            if (files != null) for (File f : files) f.delete();
        } catch (Exception ignored) { }
    }

    /** جسر JavaScript: حفظ النسخة الاحتياطية في مجلد التنزيلات. */
    private class Bridge {
        @JavascriptInterface
        public void saveBackup(final String text) {
            final byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            try {
                String where;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Downloads.DISPLAY_NAME, "namnam-backup.nnb");
                    v.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream");
                    v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    if (uri == null) throw new Exception("insert failed");
                    OutputStream os = getContentResolver().openOutputStream(uri);
                    if (os == null) throw new Exception("open failed");
                    os.write(bytes);
                    os.close();
                    where = "التنزيلات";
                } else {
                    File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    if (dir == null) throw new Exception("no dir");
                    dir.mkdirs();
                    File out = new File(dir, "namnam-backup.nnb");
                    FileOutputStream os = new FileOutputStream(out);
                    os.write(bytes);
                    os.close();
                    where = out.getAbsolutePath();
                }
                final String msg = "تم حفظ النسخة الاحتياطية: " + where;
                runOnUiThread(new Runnable() {
                    @Override public void run() { Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show(); }
                });
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override public void run() { Toast.makeText(MainActivity.this, "تعذّر حفظ النسخة", Toast.LENGTH_LONG).show(); }
                });
            }
        }
    }
}
