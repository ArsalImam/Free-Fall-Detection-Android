package io.xbird.sample.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.xbird.library.database.AppDatabase
import io.xbird.library.database.entity.FreeFallReading
import io.xbird.library.enum.Actions
import io.xbird.library.enum.ServiceState
import io.xbird.library.enum.getServiceState
import io.xbird.library.service.MotionDetectService
import io.xbird.library.utils.log
import io.xbird.sample.R
import io.xbird.sample.databinding.ActivityMainBinding
import io.xbird.sample.viewmodels.MainViewModel
import io.xbird.sample.widgets.LastAdapter
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private var viewModel: MainViewModel? = null


    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel?.updateReadings()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        viewModel = ViewModelProvider.NewInstanceFactory().create(MainViewModel::class.java)
        viewModel?.onCreate(AppDatabase.getAppDataBase(this))

        binding?.viewModel = viewModel
        setupRecyclerView()
        setupService(Actions.START)
    }

    private fun setupRecyclerView() {
        readingsRecyclerView.adapter = LastAdapter(R.layout.adapter_main,
            object : LastAdapter.OnItemClickListener<FreeFallReading> {
                override fun onItemClick(item: FreeFallReading) {

                }
            })

        viewModel?.freeFallReading?.observe(this, Observer {
            (readingsRecyclerView.adapter as LastAdapter<FreeFallReading>).items = it
        })
        viewModel?.loading?.observe(this, Observer {
            swipeToRefresh.isRefreshing = it
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel?.destroy()
    }

    private fun setupService(action: Actions) {
        if (getServiceState(this) == ServiceState.STOPPED && action == Actions.STOP) return
        Intent(this, MotionDetectService::class.java).also {
            it.action = action.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                log("Starting the service in >=26 Mode")
                startForegroundService(it)
                return
            }
            log("Starting the service in < 26 Mode")
            startService(it)
        }

        //handle receiver
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.registerReceiver(
            broadcastReceiver,
            IntentFilter(MotionDetectService.ACTION_NEW_FALL_DETECTED)
        )
    }
}