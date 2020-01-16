package com.binbo_kodakusan.youtuberoid

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// EditTextにEditableを設定するために拡張メソッドで文字列から変換
fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

class MainActivity : AppCompatActivity() {

    // コントロールのセットアップ
    private fun setupControls() {
        // ボタンの処理
        // ダウンロードボタン
        findViewById<Button>(R.id.btnDownload)?.setOnClickListener {
            // ビデオID取得
            val videoIds = getVideoIds(findViewById<EditText>(R.id.editId).text.toString())
            if (videoIds.isNotEmpty()) {
                with (findViewById<WebView>(R.id.webView)) {
                    loadUrl("https://www.youtube.com/watch?v=" + videoIds[0])
                }
            }
            with(findViewById<TextView>(R.id.textTitle)) {
                val toast = Toast.makeText(this@MainActivity, "ダウンロードします: " + text, Toast.LENGTH_SHORT)
                toast.show()
                text = "ダウンロード開始します".toEditable()
            }
            // ビデオダウンロード
            DownloadTask(this).execute(*videoIds)
        }
    }

    // YouTube表示用WebViewのセットアップ
    private fun setupWebViewForYouTube() {
        // WebViewの処理
        // 勝手にYouTubeアプリで開いてしまうので抑止
        val webView = findViewById<WebView>(R.id.webView)
        webView.setWebViewClient(object: WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                Log.d(this@MainActivity.toString(), "shouldOverrideUrlLoading: " + request?.url.toString())
                request?.url?.toString()?.also { uri ->
                    with(findViewById<EditText>(R.id.editId)) {
                        val oldUrl = text.toString()
                        if (uri != oldUrl) {
                            Log.d(this@MainActivity.toString(), "$oldUrl -> $uri")
                            text = uri.toEditable()
                            // タイトル取得
                            val videoIds = getVideoIds(uri)
                            GetTitleTask(this@MainActivity).execute(*videoIds)
                        }
                    }
                }
                return false
            }
        })
        // WebViewの設定
        val settings = webView.getSettings()
        settings.setJavaScriptEnabled(true);
        settings.setPluginState(WebSettings.PluginState.ON)
    }

    // アプリケーションの状態を復元
    private fun restoreStateFromBundle(savedInstanceState: Bundle?) {
        // 状態を復元する(インテントの処理より先にやること)
        IntentUtil.getStateFromBundle(savedInstanceState)?.also { state ->
            with(findViewById<EditText>(R.id.editId)) {
                text = state.videoIds.toEditable()
            }
        }
    }

    // 他のアプリケーションから送られたインテントを処理
    private fun setStateFromOtherApplication() {
        // インテントの処理
        intent?.also { intent ->
            Log.d(this.toString(), "onCreate: $intent")
            if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                // ブラウザからの共有で呼ばれた時の処理
                // intent: Intent { act=android.intent.action.SEND typ=text/plain flg=0x10480001 cmp=com.binbo_kodakusan.youtuberoid/.MainActivity clip={text/plain T:https://youtu.be/_VH91mivTUw} (has extras) }
                intent.clipData?.getItemAt(0)?.text?.toString()?.also { uri ->
                    with(findViewById<EditText>(R.id.editId)) {
                        text = uri.toEditable()
                        // タイトル取得
                        val videoIds = getVideoIds(uri)
                        GetTitleTask(this@MainActivity).execute(*videoIds)
                    }
                }
            }
        }
    }

    // アプリケーションの状態を保存
    private fun saveStateToBundle(outState: Bundle) {
        // 状態を保存する
        with(findViewById<EditText>(R.id.editId)) {
            val videoIds = text.toString()
            IntentUtil.setStateToBundle(InstanceState(videoIds), outState)
        }
    }

    // アクティビティ作成時に呼ばれる
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupControls()
        setupWebViewForYouTube()
        restoreStateFromBundle(savedInstanceState)
        setStateFromOtherApplication()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveStateToBundle(outState)
    }

    // URL(かvideoIdそのもの)からvideoIdを取得
    private fun getVideoIds(s: String): Array<String> {
        Log.d(this.toString(), "getVideoIds: $s")
        if (s.startsWith("http")) {
            // "v="がついていたらその後ろから'&'の前までがid
            // https://www.youtube.com/watch?v=_VH91mivTUw
            val start = s.indexOf("v=")
            if (start >= 0) {
                val rs = arrayOf(s.substring(start + 2).substringBefore('&'))
                for (t in rs)
                    Log.d(this.toString(), t)
                return rs
            }
            // 最後の'/'の後ろがid
            // ex) https://youtu.be/_VH91mivTUw
            val end = s.lastIndexOf('/')
            if (end >= 0) {
                val rs = arrayOf(s.substring(end + 1))
                for (t in rs)
                    Log.d(this.toString(), t)
                return rs
            }
        }
        // httpで始まっていなければidそのもの
        // ex) _VH91mivTUw
        val rs = arrayOf(s)
        for (t in rs)
            Log.d(this.toString(), t)
        return rs
    }
}
