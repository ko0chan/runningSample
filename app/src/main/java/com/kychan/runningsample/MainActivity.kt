package com.kychan.runningsample

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.kychan.runningsample.databinding.ActivityMainBinding
import com.naver.maps.map.MapFragment
import java.util.*
class LocationTest {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    fun checkPermission(context: Context): Boolean {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false
        } else {
            //
            return true
        }
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private var timerTask: Timer? = null
    private var isRunning = false
    private var time = 0
    var distance = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createLocationRequest()

        var temp: Location? = null
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    // Update UI with location data
                    if (temp != null) {
                        distance += location.distanceTo(temp)
                        binding.distance2.text = distance.toString()

                        binding.speed2.text = (location.speed * 3.6).toString() + "km/h"
                        val speed = (location.speed * 3.6)
                        binding.speed2.text = String.format("%.2f km/h", speed)
                        val time = 3600 / speed // 초단위 sec
                        val min = (time / 60).toInt()
                        val sec = (time % 60).toInt()
                        binding.time2.text = "${min}' ${sec}\""
                    }
                    temp = location
                }
            }
        }
        /* 평균 속도를 구하자*/
        binding.start.setOnClickListener {
            isRunning = !isRunning
            if (isRunning) start() else pause()
        }
        binding.reset.setOnClickListener {
            reset()
        }
        setMapView()
    }

    fun setMapView() {
        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.map_fragment, it).commit()
            }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 2000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(this@MainActivity,
                        REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (true) startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
    }

//    fun permissionCheck() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
//            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            return
//        }
//    }

    private fun start() {
        binding.start.text = "중지"
//        Timer().schedule(object : TimerTask() {
//            override fun run() {
//                runOnUiThread {
//                    binding.timer.text = (binding.timer.text.toString().toInt()+1).toString()
//                }
//            }
//        }, 1000, 1000)
        timerTask = kotlin.concurrent.timer(period = 1000) { //반복주기는 peroid 프로퍼티로 설정, 단위는 1000분의 1초 (period = 1000, 1초)
            time++ // period=10으로 0.01초마다 time를 1씩 증가하게 됩니다
            val min = time / 60 // time/100, 나눗셈의 몫 (초 부분)
            val sec = time % 60 // time%100, 나눗셈의 나머지 (밀리초 부분)

            // UI조작을 위한 메서드
            runOnUiThread {
                binding.timer.text = "$min : $sec"
            }
        }

    }

    private fun pause() {
        binding.start.text = "재실행"
        timerTask?.cancel()
    }

    private fun reset() {
        timerTask?.cancel() // timerTask가 null이 아니라면 cancel() 호출

        time = 0 // 시간저장 변수 초기화
        isRunning = false // 현재 진행중인지 판별하기 위한 Boolean변수 false 세팅
        binding.timer.text = "0 : 00"

        binding.start.text ="시작"

        //평균속도
        (distance / time) * 3.6
    }
    companion object {
        protected const val REQUEST_CHECK_SETTINGS = 9001
    }
}