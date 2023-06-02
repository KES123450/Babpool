package com.example.matchingproto

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.matchingproto.databinding.ActivitySucceedMatchBinding
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class SucceedMatchActivity : AppCompatActivity(), OnMapReadyCallback {
    var PERM_FLAG = 99
    val permission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    lateinit var providerClinet : FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    lateinit var apiClient : GoogleApiClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    var myID = "KGU"        // 임시로 설정해둔 내ID
    lateinit var mateID:String
    lateinit var matchBinding: ActivitySucceedMatchBinding
    private val interval = 1000
    private val handler = Handler()
    val userLocDB: FirebaseFirestore = FirebaseFirestore.getInstance()
    var mylatitude:Double =0.0
    var mylongitude:Double=0.0
    var matelatitude:Double =0.0
    var matelongitude:Double=0.0
    var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mateID = intent.getStringExtra("mateID").toString()
        myID= intent.getStringExtra("myID").toString()
        //setContentView(R.layout.activity_succeed_match)

        matchBinding = ActivitySucceedMatchBinding.inflate(layoutInflater)
        var view = matchBinding.root
        setContentView(view)

        matchBinding.chatBtn.setOnClickListener {
            // TODO: 채팅 화면으로 이동
            /*
            val intent = Intent(this@SucceedMatchActivity, ChatActivity::class.java)
            startActivity(intent)
            */
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(10 * 1000) // 10 seconds
            .setFastestInterval(1 * 1000) // 1 second
        setContentView(matchBinding.root)
        (supportFragmentManager.findFragmentById(R.id.map) as
                SupportMapFragment?)!!.getMapAsync(this)
        getMateLoc()
        val runnable = object : Runnable {
            override fun run() {
                Log.d("log", mylatitude.toString())

                setUserLoc()

                googleMap?.clear()
                makeMarker(mylatitude, mylongitude, myID)
                makeMarker(matelatitude, matelongitude, mateID)

                // 일정 시간 간격으로 다시 실행
                handler.postDelayed(this, interval.toLong())
            }
        }
        handler.postDelayed(runnable, interval.toLong())
        providerClinet = LocationServices.getFusedLocationProviderClient(this@SucceedMatchActivity)

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
            if(ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
                return false
        }
        return true
    }
    fun startProcess(){
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // 상대방의 위치정보를 받아서 내 앱에 업데이트
    private fun getMateLoc(){
        userLocDB.collection("User_Loc")
            .document(mateID)
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Log.d("Firestore", "Listen failed: $e")
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val documentData = documentSnapshot.data
                    matelatitude = documentData?.get("latitude") as Double
                    matelongitude = documentData?.get("longitude") as Double

                } else {
                    Log.d("Firestore", "Document does not exist")
                }
            }
    }
    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                    this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback()
        {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.lastLocation?.let {
                    mylatitude = it.latitude
                    mylongitude = it.longitude
                // do something with latitude and longitude
                }
            } }, null)
    }

    //내 위치정보를 fetchLocation으로 받아서 파이어베이스에 업데이트
    private fun setUserLoc(){
        fetchLocation()

        userLocDB.collection("User_Loc")
            .document(myID)
            .update(mapOf(
                "latitude" to mylatitude,
                "longitude" to mylongitude
            ))

    }
    private fun makeMarker(latitude:Double,longitude:Double,ID:String){
        //마커표시
        val markerOption= MarkerOptions()
        markerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker2))
        markerOption.position(LatLng(latitude, longitude))
        markerOption.title(ID)

        googleMap?.addMarker(markerOption)
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
                    if(grant != PackageManager.PERMISSION_GRANTED){
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
    override fun onMapReady(p0: GoogleMap?) {
        googleMap = p0
    }
    // TODO: 상대방 현재 위치를 받아서 지도에 표시
}