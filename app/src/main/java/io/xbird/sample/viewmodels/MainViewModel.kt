package io.xbird.sample.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.xbird.library.database.AppDatabase
import io.xbird.library.database.entity.FreeFallReading
import io.xbird.library.utils.log

class MainViewModel : ViewModel() {

    private var appDataBase: AppDatabase? = null

    private val _freeFallReading =
        MutableLiveData<List<FreeFallReading>>().apply { value = ArrayList() }
    val freeFallReading: LiveData<List<FreeFallReading>>
        get() = _freeFallReading

    private val _loading =
        MutableLiveData<Boolean>().apply { value = false }
    val loading: LiveData<Boolean>
        get() = _loading


    fun onCreate(appDataBase: AppDatabase?) {
        this.appDataBase = appDataBase
        updateReadings()
    }

    fun onRefresh() {
        updateReadings()
    }

    fun updateReadings() {
        _loading.value = true
        appDataBase?.freeFallReadingDao()?.getLatestReadings()
            ?.observeOn(AndroidSchedulers.mainThread())?.subscribeOn(Schedulers.io())
            ?.subscribe({
                _loading.value = false
                _freeFallReading.value = it
            }, {
                _loading.value = false
                log("No data found")
                it.printStackTrace()
            })
    }

    fun destroy() {
        AppDatabase.destroyDataBase()
    }
}