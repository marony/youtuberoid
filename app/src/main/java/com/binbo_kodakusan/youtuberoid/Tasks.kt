package com.binbo_kodakusan.youtuberoid

import android.app.Activity
import android.os.AsyncTask
import android.util.Log
import android.widget.TextView
import com.github.kiulian.downloader.YoutubeDownloader

// 非同期タスク
// videoIdからバックグラウンドで動画のダウンロードを行う
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

// 非同期タスク
// videoIdからバックグラウンドで動画のタイトルを取得する
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