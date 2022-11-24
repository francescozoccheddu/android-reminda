package francescozoccheddu.reminda

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.media.AudioAttributes
import android.provider.Settings
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

object Alarmer {

    data class Alarm(val time: LocalDateTime, val title: String)

    private const val REQUEST_CODE : Int = 0
    const val ACTION : String = "francescozoccheddu.reminda.ALARM"
    private const val PREFS_NAME = "francescozoccheddu.reminda.PREFS"
    private const val PREFS_NEXT_ALARM_INDEX_KEY = "nextAlarmIndex"
    private const val NOTIFICATION_CHANNEL_ID = "francescozoccheddu.reminda.NOTIFICATION"

    var nextAlarmIndex : Int = 0
        private set

    private var pendingIntent : PendingIntent? = null
    private lateinit var alarmManager: AlarmManager
    private lateinit var prefsEditor: SharedPreferences.Editor
    private lateinit var notificationManager: NotificationManager
    private val timeFormatter : DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())

    val alarms : List<Alarm> = listOf(
        Alarm(LocalDateTime.of(2022, Month.NOVEMBER, 25, 15, 28), "First alarm"),
        Alarm(LocalDateTime.of(2022, Month.NOVEMBER, 25, 15, 29), "Second alarm"),
        Alarm(LocalDateTime.of(2022, Month.NOVEMBER, 25, 15, 30), "Third alarm"),
    ).sortedBy { it.time }

    private fun next(context: Context) {
        if (nextAlarmIndex >= alarms.size) {
            throw RuntimeException("No next alarm")
        }
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent = null
        }
        val notification = Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(alarms[nextAlarmIndex].title)
            .setContentText(timeFormatter.format(alarms[nextAlarmIndex].time))
            .setCategory(Notification.CATEGORY_ALARM)
            .setWhen(alarms[nextAlarmIndex].time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            .setShowWhen(true)
            .build()
        notificationManager.notify(nextAlarmIndex, notification)
        nextAlarmIndex++
    }

    private fun delayNext(context: Context) {
        if (nextAlarmIndex >= alarms.size) {
            throw RuntimeException("No next alarm")
        }
        if (pendingIntent != null) {
            throw RuntimeException("Pending intent")
        }
        val intent = Intent(context, AlarmBroadcastReceiver::class.java)
        intent.action = ACTION
        pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val timeMs = ChronoUnit.MILLIS.between(LocalDateTime.now(), alarms[nextAlarmIndex].time) + System.currentTimeMillis()
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
    }

    private fun initialize(context: Context) {
        if (!this::alarmManager.isInitialized) {
            alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        }
        if (!this::prefsEditor.isInitialized) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            nextAlarmIndex = prefs.getInt(PREFS_NEXT_ALARM_INDEX_KEY, 0)
            prefsEditor = prefs.edit()
        }
        if (!this::notificationManager.isInitialized) {
            notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val name = context.getString(R.string.notification_channel_name)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                enableLights(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                enableVibration(true)
                lightColor = Color.MAGENTA
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun update(context: Context) {
        initialize(context)
        val now = LocalDateTime.now()
        while (nextAlarmIndex < alarms.size && alarms[nextAlarmIndex].time <= now) {
            next(context)
        }
        prefsEditor.putInt(PREFS_NEXT_ALARM_INDEX_KEY, nextAlarmIndex)
        prefsEditor.apply()
        if (nextAlarmIndex < alarms.size && pendingIntent == null) {
            delayNext(context)
        }
    }

    fun fire(context: Context) {
        initialize(context)
        next(context)
        update(context)
    }

}