package com.example.matchingproto

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
//import android.location.LocationRequest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.se.omapi.SEService.OnConnectedListener
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.matchingproto.databinding.ActivityAutoMatchingBinding
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.FirebaseFirestore

class AutoMatchingActivity : AppCompatActivity(), OnMapReadyCallback {
    //var googleMap: GoogleMap? = null
    val DB: FirebaseFirestore = FirebaseFirestore.getInstance()
    var PERM_FLAG = 99
    val permission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    lateinit var providerClinet : FusedLocationProviderClient
    lateinit var myID:String
    private lateinit var mBinding: ActivityAutoMatchingBinding
    private lateinit var mMap: GoogleMap
    lateinit var apiClient : GoogleApiClient
    var latitude:Double =0.0
    var longitude:Double=0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_auto_matching)

        // Activity Binding 객체에 할당 및 View 설정
        mBinding = ActivityAutoMatchingBinding.inflate(layoutInflater)
        val view = mBinding.root
        setContentView(view)
        //userid가져오기
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        myID = sharedPreferences.getString("email", "").toString()


        mBinding.matchBtn.setOnClickListener {
            DB.collection("auto")
                .document(myID)
                .set(mapOf("participantID" to "tmp",
                    "participate_find" to false,
                    "matchingCheck" to "WAIT"))

            val intent = Intent(this@AutoMatchingActivity, WaitAutoParticiant::class.java)
            startActivity(intent)
        }
        mBinding.backBtn.setOnClickListener {
            val intent = Intent(this@AutoMatchingActivity, Main_login::class.java)
            startActivity(intent)
        }
        providerClinet = LocationServices.getFusedLocationProviderClient(this@AutoMatchingActivity)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if(isPermitted()){
            startProcess()
        }
        else {
            ActivityCompat.requestPermissions(this, permission, PERM_FLAG)
        }

    }
    fun isPermitted() : Boolean{
        for(perm in permission) {
            if(ContextCompat.checkSelfPermission(this, perm) != PERMISSION_GRANTED)
                return false
        }
        return true
    }
    fun startProcess(){
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    override fun onMapReady(googleMap : GoogleMap) {
        mMap = googleMap
        /*
        val kgu = LatLng(37.3012072, 127.0388322)
        mMap.addMarker(MarkerOptions().position(kgu).title("Marker in KGU"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(kgu))
        */
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        setUpdateLocationListener()
    }

    lateinit var fusedLocationProviderClient:FusedLocationProviderClient
    lateinit var locationCallback:LocationCallback

    @SuppressLint("MissingPermission")
    fun setUpdateLocationListener(){
        val locationRequest = LocationRequest.create()
        locationRequest.run{
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult:LocationResult?){
                locationResult?.let {
                    for((i, location) in it.locations.withIndex()){
                        Log.d("로케이션", "$i ${location.latitude}, ${location.altitude}")
                        setlocation(location)
                    }
                }
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    fun setlocation(location:Location){
        val myLocation = LatLng(location.latitude, location.longitude)
        val markerOption =  MarkerOptions()
            .position(myLocation)
            .title("You are here")
        val cameraOption = CameraPosition.Builder()
            .target(myLocation)
            .zoom(15.0f)
            .build()
        val camera = CameraUpdateFactory.newCameraPosition(cameraOption)

        mMap.clear()
        mMap.addMarker(markerOption)
        mMap.moveCamera(camera)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERM_FLAG -> {
                var check = false
                for(grant in grantResults){
                    if(grant != PERMISSION_GRANTED){
                        check = false
                        break
                    }
                }
                if(check){
                    startProcess()
                }
                else {
                    Toast.makeText(this, "권한을 허용해야 어플 사용이 가능합니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

    }

}