package io.github.takusan23.thermalmonitor

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 発熱状態を取得できるコールバックを購読するサービス
 */
class ThermalMonitorService : Service() {

    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private val powerManager by lazy { getSystemService(Context.POWER_SERVICE) as PowerManager }

    /** サービス終了用ブロードキャスト */
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            stopSelf()
        }
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // 通知チャンネル登録
        registerNotificationChannel()
        // 通知発行
        postNotification()
        // BroadcastReceiver設定
        val intentFilter = IntentFilter().apply {
            addAction(SERVICE_STOP_INTENT_FILTER)
        }
        registerReceiver(broadcastReceiver, intentFilter)

        powerManager.addThermalStatusListener {
            val (type, message) = convertText(it)
            postNotification(type, message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    private fun convertText(status: Int) = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> "THERMAL_STATUS_NONE" to "スロットル中ではありません"
        PowerManager.THERMAL_STATUS_LIGHT -> "THERMAL_STATUS_LIGHT" to "UX に影響しないライト スロットリング"
        PowerManager.THERMAL_STATUS_MODERATE -> "THERMAL_STATUS_MODERATE" to "UX に大きな影響がない中程度の調整。"
        PowerManager.THERMAL_STATUS_SEVERE -> "THERMAL_STATUS_SEVERE" to "UX に大きな影響を与える深刻なスロットリング"
        PowerManager.THERMAL_STATUS_SHUTDOWN -> "THERMAL_STATUS_SHUTDOWN" to "すぐにシャットダウンする必要があります"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "THERMAL_STATUS_EMERGENCY" to "温度状態により、プラットフォームの主要コンポーネントがシャットダウンしています。デバイスの機能が制限されます。"
        PowerManager.THERMAL_STATUS_CRITICAL -> "THERMAL_STATUS_CRITICAL" to "プラットフォームは、電力を削減するためにあらゆることを行いました。"
        else -> "NONE" to "問題が発生しました"
    }

    private fun postNotification(title: String = "発熱通知", contentText: String = "発熱コールバック通知") {
        val stopServicePendingIntent = PendingIntent.getBroadcast(this, 100, Intent(SERVICE_STOP_INTENT_FILTER), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setContentTitle(title)
            setContentText(contentText)
            setSmallIcon(R.drawable.ic_outline_thermostat_24)
            addAction(R.drawable.ic_outline_thermostat_24, "終了", stopServicePendingIntent)
        }.build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun registerNotificationChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManager.IMPORTANCE_LOW).apply {
                setName("発熱コールバック通知")
            }.build()
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    companion object {

        private const val CHANNEL_ID = "thermal_monitor_notification"
        private const val NOTIFICATION_ID = 8080
        private const val SERVICE_STOP_INTENT_FILTER = "stop_service"

    }

}