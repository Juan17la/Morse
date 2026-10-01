package com.morse.wearos.transport

import android.content.Context
import com.morse.wearos.R
import com.morse.wearos.data.DefaultWords

/** Demo builds ignore the connection setting and always use fake members. */
const val IS_DEMO = true

fun demoTransport(context: Context): GroupTransport? = SimulatedTransport(
    fakeNames = listOf(R.string.demo_member_ana, R.string.demo_member_luis, R.string.demo_member_maria)
        .map(context::getString),
    replyWords = DefaultWords.resources.map(context::getString),
)
