package com.binbo_kodakusan.youtuberoid

import android.app.Activity
import android.content.Intent.ACTION_SEND
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.github.kiulian.downloader.YoutubeDownloader
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    // EditTextにEditableを設定するために拡張メソッドで文字列から変換
    fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupControls()
        setupWebViewForYouTube()

        // 状態を復元する(インテントの処理より先にやること)
        if (savedInstanceState != null) {
            val editText = findViewById<EditText>(R.id.editId)
            if (editText != null) {
                editText.text = savedInstanceState.getString("videoIds", "").toEditable()
            }
        }

        // インテントの処理
        if (intent != null) {
            println("onResume: " + intent)
            if (intent.action == ACTION_SEND && intent.type == "text/plain") {
                // ブラウザからの共有で呼ばれた時の処理
                // intent: Intent { act=android.intent.action.SEND typ=text/plain flg=0x10480001 cmp=com.binbo_kodakusan.youtuberoid/.MainActivity clip={text/plain T:https://youtu.be/_VH91mivTUw} (has extras) }
                intent.clipData?.getItemAt(0).let {
                    val uri = it?.text
                    val editText = findViewById<EditText>(R.id.editId)
                    if (editText != null)
                        editText.text = uri.toString().toEditable()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 状態を保存する
        val editText = findViewById<EditText>(R.id.editId)
        val videoIds = editText.text.toString()
        if (videoIds != null)
            outState.putString("videoIds", videoIds)
    }

    private fun getVideoIds(s: String): Array<String> {
        if (s.startsWith("http")) {
            // "v="がついていたらその後ろから'&'の前までがid
            // https://www.youtube.com/watch?v=_VH91mivTUw
            val start = s.indexOf("v=")
            if (start >= 0)
                return arrayOf(s.substring(start + 2).substringBefore('&'))
            // 最後の'/'の後ろがid
            // ex) https://youtu.be/_VH91mivTUw
            val end = s.lastIndexOf('/')
            if (end >= 0)
                return arrayOf(s.substring(end + 1))
        }
        // httpで始まっていなければidそのもの
        // ex) _VH91mivTUw
        return arrayOf(s)
    }

    private fun setupControls() {
        // ボタンの処理
        // ダウンロードボタン
        val button = findViewById<Button>(R.id.btnDownload)
        button.setOnClickListener {
            // ビデオID取得
            val videoIds = getVideoIds(findViewById<EditText>(R.id.editId).text.toString())
            if (videoIds.size > 0) {
                val webView = findViewById<WebView>(R.id.webView)
                webView.loadUrl("https://www.youtube.com/watch?v=" + videoIds[0])
            }
            // ビデオダウンロード
            DownloadTask(this).execute(*videoIds)
        }
    }

    private fun setupWebViewForYouTube() {
        // WebViewの処理
        // 勝手にYouTubeアプリで開いてしまうので抑止
        val webView = findViewById<WebView>(R.id.webView)
        webView.setWebViewClient(object: WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        })
        // WebViewの設定
        val settings = webView.getSettings()
        settings.setJavaScriptEnabled(true);
        settings.setPluginState(WebSettings.PluginState.ON)
    }
}

class DownloadTask(val activity: Activity) : AsyncTask<String, String, String>() {
    override fun doInBackground(vararg params: String?): String {
        println("doInBackground: " + params)
        for (videoId: String? in params) {
            if (videoId != null) {
                println("videoId: " + videoId)
                try {
                    val video = YoutubeDownloader.getVideo(videoId)
                    val details = video.details()
                    val formats = video.formats()
                    val format = formats.maxBy { f -> f.contentLength() }
                    val id = video.download(format, activity)
                    val snackbar = Snackbar.make(activity.requireViewById(R.id.btnDownload), details.title() + "をダウンロードします", Snackbar.LENGTH_SHORT)
                    snackbar.show()
                    println("completed: " + id)
                }
                catch (e: Throwable) {
                    println("videoId: " + e)
                }
            }
        }
        return "success"
    }

    override fun onPostExecute(result: String) {
        println("onPostExecute: " + result)
    }
}