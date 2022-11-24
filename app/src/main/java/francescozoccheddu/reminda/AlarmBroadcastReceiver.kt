package francescozoccheddu.reminda

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Alarmer.ACTION) {
            Alarmer.fire(context.applicationContext)
        }
    }

}