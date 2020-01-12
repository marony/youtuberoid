package com.binbo_kodakusan.youtuberoid

import android.os.Bundle

class InstanceState(val videoIds: String)

class IntentUtil {
    companion object {
        private const val IdVideoIds = "videoIds"

        fun getStateFromBundle(bundle: Bundle?): InstanceState? {
            bundle?.let {
                val videoIds = it.getString(IdVideoIds, "")
                return InstanceState(videoIds)
            }
            return null
        }

        fun setStateToBundle(state: InstanceState, bundle: Bundle) {
            bundle.putString(IdVideoIds, state.videoIds)
        }
    }
}