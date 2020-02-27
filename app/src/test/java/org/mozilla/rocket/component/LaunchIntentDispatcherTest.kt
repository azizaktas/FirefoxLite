package org.mozilla.rocket.component

import android.app.Application
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.notification.RocketMessagingService.Companion.STR_PUSH_COMMAND
import org.mozilla.focus.notification.RocketMessagingService.Companion.STR_PUSH_OPEN_URL
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class LaunchIntentDispatcherTest {

    @Test
    fun dispatch() {
        val command = Intent()
        command.putExtra(STR_PUSH_COMMAND, LaunchIntentDispatcher.Command.SET_DEFAULT.value)
        test(command, LaunchIntentDispatcher.Action.HANDLED)

        val view = Intent()
        view.putExtra(STR_PUSH_OPEN_URL, "https://mozilla.com")
        test(view, LaunchIntentDispatcher.Action.NORMAL)

        val launch = Intent()
        launch.action = ACTION_MAIN
        test(launch, LaunchIntentDispatcher.Action.NORMAL)
    }

    fun test(intent: Intent, value: LaunchIntentDispatcher.Action) {
        ApplicationProvider.getApplicationContext<Application>().apply {
            val dispatch = LaunchIntentDispatcher.dispatch(this, intent)
            assert(dispatch == value)
        }
    }
}