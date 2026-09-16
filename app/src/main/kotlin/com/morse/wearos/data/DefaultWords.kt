package com.morse.wearos.data

import android.content.Context
import androidx.annotation.StringRes
import com.morse.wearos.R

object DefaultWords {
    @StringRes
    val resources: List<Int> = listOf(
        R.string.word_yes,
        R.string.word_no,
        R.string.word_maybe,
        R.string.word_im_here,
        R.string.word_wait,
    )

    fun resolve(context: Context): List<String> = resources.map(context::getString)
}
