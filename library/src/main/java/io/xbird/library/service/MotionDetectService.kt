package io.xbird.library.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.xbird.library.BuildConfig
import io.xbird.library.database.AppDatabase
import io.xbird.library.database.entity.FreeFallReading
import io.xbird.library.enum.Actions
import io.xbird.library.enum.ServiceState
import io.xbird.library.enum.setServiceState
import io.xbird.library.utils.FreeFallPolicyManager
import io.xbird.library.utils.createNotification
import io.xbird.library.utils.log
import java.util.*
import kotlin.math.roundToInt


class MotionDetectService : Service() {

    private lateinit var sensorManager: SensorManager
    private val myBinder = MyLocalBinder()
    private var sensor: Sensor? = null
    private var freeFallPolicyManager: FreeFallPolicyManager? = null
    private var isServiceStarted = false
    private val minTimeToNotifyAgain = 1 * 1000 // 1 sec
    private var lastSentInMillis = 0L

    private val sensorListener: SensorEventListener? = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (freeFallPolicyManager?.isValidFall(event) == true) {
                log("fall detected")
                if (isOkayToNotifyAgain()) {
                    lastSentInMillis = System.currentTimeMillis()
                    log("timestamp => ${event?.timestamp!!}")
                    val duration: Long =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            System.currentTimeMillis() - (System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000)
                        } else {
                            System.currentTimeMillis() - (System.currentTimeMillis() + (event.timestamp - System.nanoTime()) / 1000000)
                        }
                    log("duration => ${duration}")
                    insertToDb(event?.timestamp!!, duration)
                    notifyFall(duration)
                }
            }
        }

        private fun insertToDb(timestamp: Long, duration: Long) {
            Completable.fromAction {
                AppDatabase.getAppDataBase(this@MotionDetectService)?.freeFallReadingDao()?.insert(
                    FreeFallReading(
                        createdDate = Date(),
                        duration = duration,
                        timestamp = timestamp
                    )
                )
            }.observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(object : CompletableObserver {

                    override fun onComplete() {
                        val localBroadcastManager =
                            LocalBroadcastManager.getInstance(this@MotionDetectService)
                        val localIntent = Intent(ACTION_NEW_FALL_DETECTED)
                        localBroadcastManager.sendBroadcast(localIntent)
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                    }
                })
        }
    }

    private fun notifyFall(duration: Long) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(
            (System.currentTimeMillis() / 1000).toDouble().roundToInt(),
            createNotification(
                this,
                notificationManager,
                title = "Fall detected",
                description = "fall detection duration: $duration ms",
                isCancelable = true,
                notificationChannelId = "${BuildConfig.APPLICATION_ID}.detection.received"
            )
        )
    }

    private fun isOkayToNotifyAgain(): Boolean {
        return lastSentInMillis == 0L || this.lastSentInMillis + this.minTimeToNotifyAgain < System.currentTimeMillis()
    }

    override fun onBind(intent: Intent): IBinder? {
        return myBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("onStartCommand executed with startId: $startId")
        if (intent != null) {
            val action = intent.action
            log("using an intent with action $action")
            when (action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
                else -> log("This should never happen. No action in the received intent")
            }
        } else {
            log(
                "with a null intent. It has been probably restarted by the system."
            )
        }
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        log("The service has been created".toUpperCase())

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val toStopService = Intent(this, MotionDetectService::class.java)
        toStopService.action = Actions.STOP.name

        val notification = createNotification(
            this, notificationManager, isCancelable = false,
            pendingIntent = PendingIntent.getService(
                this,
                0,
                toStopService,
                PendingIntent.FLAG_CANCEL_CURRENT
            )
        )

        startForeground(SERVICE_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        log("The service has been destroyed".toUpperCase())
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show()
    }

    private fun startService() {
        if (isServiceStarted) return
        log("Starting the foreground service task")
        Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
        setServiceState(this, ServiceState.STARTED)

        freeFallPolicyManager = FreeFallPolicyManager()
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        sensorManager.registerListener(
            sensorListener,
            sensor,
            SensorManager.SENSOR_DELAY_UI
        )
    }

    private fun stopService() {
        log("Stopping the foreground service")
        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
        try {
            sensorManager.unregisterListener(sensorListener, sensor)
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            log("Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)
    }

    inner class MyLocalBinder : Binder() {
        fun getService(): MotionDetectService {
            return this@MotionDetectService
        }
    }

    companion object {
        val ACTION_NEW_FALL_DETECTED: String = "ACTION_NEW_FALL_DETECTED"
        private val SERVICE_NOTIFICATION_ID: Int = 1
    }
}