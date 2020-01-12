package com.binbo_kodakusan.youtuberoid

import android.app.Activity
import android.content.Intent.ACTION_SEND
import android.os.AsyncTask
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
import com.github.kiulian.downloader.YoutubeDownloader

// EditTextにEditableを設定するために拡張メソッドで文字列から変換
fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

class MainActivity : AppCompatActivity() {

    // コントロールのセットアップ
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
            val textTitle = findViewById<TextView>(R.id.textTitle)
            textTitle.text = "ダウンロード開始します".toEditable()
            // ビデオダウンロード
            DownloadTask(this).execute(*videoIds)
        }
    }

    // YouTube表示用WebViewのセットアップ
    private fun setupWebViewForYouTube() {
        val that  = this
        // WebViewの処理
        // 勝手にYouTubeアプリで開いてしまうので抑止
        val webView = findViewById<WebView>(R.id.webView)
        webView.setWebViewClient(object: WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                Log.d(this.toString(), "shouldOverrideUrlLoading: " + request?.url.toString())
                request?.url?.let {
                    val editText = findViewById<EditText>(R.id.editId)
                    val oldUrl = editText.text.toString()
                    if (it.toString() != oldUrl) {
                        Log.d(this.toString(), oldUrl + " -> " + it.toString())
                        editText.text = it.toString().toEditable()
                        // タイトル取得
                        val videoIds = getVideoIds(it.toString())
                        GetTitleTask(that).execute(*videoIds)
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
        IntentUtil.getStateFromBundle(savedInstanceState)?.let {
            val editText = findViewById<EditText>(R.id.editId)
            if (editText != null) {
                val toast = Toast.makeText(this, "状態復元: " + it, Toast.LENGTH_SHORT)
                toast.show()
                editText.text = it.videoIds.toEditable()
            }
        }
    }

    // 他のアプリケーションから送られたインテントを処理
    private fun setStateFromOtherApplication() {
        val that = this
        // インテントの処理
        intent?.let {
            Log.d(this.toString(), "onCreate: " + it)
            if (it.action == ACTION_SEND && it.type == "text/plain") {
                // ブラウザからの共有で呼ばれた時の処理
                // intent: Intent { act=android.intent.action.SEND typ=text/plain flg=0x10480001 cmp=com.binbo_kodakusan.youtuberoid/.MainActivity clip={text/plain T:https://youtu.be/_VH91mivTUw} (has extras) }
                it.clipData?.getItemAt(0)?.let {
                    val uri = it.text.toString()
                    val editText = findViewById<EditText>(R.id.editId)
                    if (editText != null) {
                        val toast = Toast.makeText(this, "Intentから入力: " + uri, Toast.LENGTH_SHORT)
                        toast.show()
                        editText.text = uri.toEditable()
                        // タイトル取得
                        val videoIds = getVideoIds(uri)
                        GetTitleTask(that).execute(*videoIds)
                    }
                }
            }
        }
    }

    // アプリケーションの状態を保存
    private fun saveStateToBundle(outState: Bundle) {
        // 状態を保存する
        val editText = findViewById<EditText>(R.id.editId)
        val videoIds = editText.text.toString()
        val toast = Toast.makeText(this, "状態保存: " + videoIds, Toast.LENGTH_SHORT)
        toast.show()
        IntentUtil.setStateToBundle(InstanceState(videoIds), outState)
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
        Log.d(this.toString(), "getVideoIds: " + s)
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

class DownloadTask(val activity: Activity) : AsyncTask<String, String, String>() {
    override fun doInBackground(vararg params: String?): String {
        Log.d(this.toString(), "DownloadTask.doInBackground: " + params[0] + ", " + params.slice(1 until params.size))
        for (videoId: String? in params) {
            videoId?.let {
                Log.d(this.toString(), "videoId: " + it)
                var id: Long
                try {
                    val video = YoutubeDownloader.getVideo(it)
                    val details = video.details()
                    val formats = video.formats()
                    val format = formats.maxBy { f -> f.contentLength() }
                    id = video.download(format, activity)
                    Log.d(this.toString(), "completed: " + id)
                }
                catch (e: Throwable) {
                    Log.d(this.toString(), e.toString())
                    return "ERROR: $e"
                }
            }
        }
        return "ダウンロード開始しました"
    }

    override fun onPostExecute(result: String) {
        Log.d(this.toString(), "DownloadTask.onPostExecute: " + result)
        if (result != "") {
            val textTitle = activity.findViewById<TextView>(R.id.textTitle)
            textTitle?.let {
                textTitle.text = result.toEditable()
            }
        }
    }
}

class GetTitleTask(val activity: Activity) : AsyncTask<String, String, String>() {
    override fun doInBackground(vararg params: String?): String {
        Log.d(this.toString(), "GetTitleTask.doInBackground: " + params[0] + ", " + params.slice(1 until params.size))
        var title = ""
        for (videoId: String? in params) {
            videoId?.let {
                Log.d(this.toString(), "videoId: " + it)
                var id = 0L
                try {
                    val video = YoutubeDownloader.getVideo(it)
                    val details = video.details()
                    title = details.title()
                    Log.d(this.toString(), "completed: " + id)
                }
                catch (e: Throwable) {
                    Log.d(this.toString(), e.toString())
                    return "ERROR: $e"
                }
            }
        }
        return title
    }

    override fun onPostExecute(result: String) {
        Log.d(this.toString(), "GetTitleTask.onPostExecute: " + result)
        if (result != "") {
            val textTitle = activity.findViewById<TextView>(R.id.textTitle)
            textTitle?.text = result.toEditable()
        }
    }
}