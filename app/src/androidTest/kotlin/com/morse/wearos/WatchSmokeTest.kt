package com.morse.wearos

import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.morse.wearos.transport.IS_DEMO
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on the watch (or a Wear emulator): `./gradlew connectedDemoDebugAndroidTest`.
 * Checks the app starts on this screen size and the core send path answers. The full build only
 * gets the launch check, since creating a group there needs a real Wi-Fi Direct or LAN peer.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class WatchSmokeTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun str(@StringRes id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    /** First launch shows the welcome (Start is its last item, so scroll to it); later launches go straight to Home. */
    private fun reachHome() {
        val home = hasText(str(R.string.home_create_group)) or hasContentDescription(str(R.string.group_send))
        val welcome = hasScrollToNodeAction()
        compose.waitUntil(10_000) { compose.onAllNodes(home or welcome).fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodes(home).fetchSemanticsNodes().isEmpty()) {
            val start = hasText(str(R.string.welcome_start))
            compose.onNode(welcome).performScrollToNode(start)
            compose.onNode(start).performClick()
        }
        compose.waitUntilAtLeastOneExists(home, 10_000)
    }

    @Test
    fun launchesToHome() = reachHome()

    @Test
    fun createGroupAndSendAWord() {
        assumeTrue("Needs the demo build's simulated group", IS_DEMO)
        reachHome()
        // A previous run may have left this watch in a group, in which case Home already offers Send.
        if (compose.onAllNodes(hasText(str(R.string.home_create_group))).fetchSemanticsNodes().isNotEmpty()) {
            compose.onAllNodes(hasText(str(R.string.home_create_group))).onFirst().performClick()
            compose.waitUntilAtLeastOneExists(hasText(str(R.string.group_send)), 10_000)
            compose.onAllNodes(hasText(str(R.string.group_send))).onFirst().performClick()
        } else {
            compose.onAllNodes(hasContentDescription(str(R.string.group_send))).onFirst().performClick()
        }

        val word = str(R.string.word_yes)
        compose.waitUntilAtLeastOneExists(hasText(word), 10_000)
        compose.onAllNodes(hasText(word)).onFirst().performClick()
        compose.waitUntilAtLeastOneExists(hasText(str(R.string.sent_title, word)), 10_000)
    }
}
