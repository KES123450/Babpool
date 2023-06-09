package com.example.matchingproto

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
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
    val userDB: FirebaseFirestore = FirebaseFirestore.getInstance()
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

        waitFinish()

        //매칭 종료 확인
        matchBinding.backBtn.setOnClickListener {
            showExitConfirmationDialog()
        }

        matchBinding.chatBtn.setOnClickListener {
            // TODO: 채팅 화면으로 이동

            val intent = Intent(this@SucceedMatchActivity, ChatActivity::class.java)
            startActivity(intent)
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
        userDB.collection("User_Loc")
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

        userDB.collection("User_Loc")
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
    override fun onBackPressed() {
        showExitConfirmationDialog()
    }

    //매칭 종료 여부 확인 창
    private fun showExitConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("매칭 종료 확인")
            .setMessage("매칭을 종료하시겠습니까?")
            .setPositiveButton("예") { dialog, which ->
                // '예' 버튼을 클릭한 경우 종료 로직을 구현
                val task1: Task<DocumentSnapshot> = userDB.collection("User_Info").document(myID).get()
                val task2: Task<DocumentSnapshot> = userDB.collection("User_Info").document(mateID).get()

                Tasks.whenAllSuccess<DocumentSnapshot>(task1, task2)
                    .addOnSuccessListener { result ->
                        // 모든 작업이 성공한 경우 결과 처리
                        val mynn = result[0].getString("Nickname")
                        val matenn = result[1].getString("Nickname")

                        // 필드 값들을 활용하여 추가 작업 수행(기록 저장)
                        val myhis: Map<String, Any> = hashMapOf<String?, String?>(matenn to matenn) as Map<String, Any>
                        val matehis: Map<String, Any> = hashMapOf<String?, String?>(mynn to mynn) as Map<String, Any>

                        val myHisDocRef = userDB.collection("User_rec").document(mynn.toString())
                        myHisDocRef.get()
                            .addOnSuccessListener { myHisDocSnapshot ->
                                if (myHisDocSnapshot.exists()) {
                                    // 기존 문서가 존재하는 경우 필드 추가 또는 업데이트
                                    myHisDocRef.update(myhis)
                                        .addOnSuccessListener {
                                            // 필드 추가 또는 업데이트 성공
                                        }
                                        .addOnFailureListener { exception ->
                                            // 필드 추가 또는 업데이트 실패
                                        }
                                } else {
                                    // 기존 문서가 존재하지 않는 경우 새로운 문서 생성
                                    myHisDocRef.set(myhis)
                                        .addOnSuccessListener {
                                            // 문서 생성 성공
                                        }
                                        .addOnFailureListener { exception ->
                                            // 문서 생성 실패
                                        }
                                }
                            }
                            .addOnFailureListener { exception ->
                                // 오류 처리
                                // 기존 문서 확인 실패
                            }

                        val mateHisDocRef = userDB.collection("User_rec").document(matenn.toString())
                        mateHisDocRef.get()
                            .addOnSuccessListener { mateHisDocSnapshot ->
                                if (mateHisDocSnapshot.exists()) {
                                    // 기존 문서가 존재하는 경우 필드 추가 또는 업데이트
                                    mateHisDocRef.update(matehis)
                                        .addOnSuccessListener {
                                            // 필드 추가 또는 업데이트 성공
                                        }
                                        .addOnFailureListener { exception ->
                                            // 필드 추가 또는 업데이트 실패
                                        }
                                } else {
                                    // 기존 문서가 존재하지 않는 경우 새로운 문서 생성
                                    mateHisDocRef.set(matehis)
                                        .addOnSuccessListener {
                                            // 문서 생성 성공
                                        }
                                        .addOnFailureListener { exception ->
                                            // 문서 생성 실패
                                        }
                                }
                            }
                            .addOnFailureListener { exception ->
                                // 오류 처리
                                // 기존 문서 확인 실패
                            }
                    }
                    .addOnFailureListener { exception ->
                        // 오류 처리
                        // 작업 중 하나라도 실패한 경우
                    }

                //위치기록 삭제
                userDB.collection("User_Loc")
                    .document(mateID)
                    .update("finish_check",true)

                userDB.collection("User_Loc")
                    .document(myID)
                    .delete()

                val intent = Intent(this, Main_login::class.java)
                startActivity(intent)
                finish()
                Toast.makeText(this, "매칭 종료!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("아니오") { dialog, which ->
                // '아니오' 버튼을 클릭한 경우 아무 동작 없음
            }
            .show()
    }

    private fun waitFinish() {
        userDB.collection("User_Loc")
            .document(myID)
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val documentData = documentSnapshot.data


                    val finishCheck = documentData?.get("finish_check") as? Boolean
                    if (finishCheck != null) {
                        // finishCheck 사용하는 코드
                        // ...
                    } else {
                        // finishCheck가 null인 경우 처리
                        // ...
                    }

                    if (finishCheck == true) {
                        userDB.collection("User_Loc")
                            .document(myID)
                            .delete()

                        //채팅삭제
                        val database = FirebaseDatabase.getInstance()
                        val reference = database.getReference("chatting") // 삭제할 데이터의 경로

                        reference.removeValue()
                            .addOnSuccessListener {
                                // 값 삭제 성공
                            }
                            .addOnFailureListener { exception ->
                                // 값 삭제 실패
                            }
                        Toast.makeText(this, "매칭 종료!", Toast.LENGTH_SHORT).show()
                        val intent: Intent = Intent(this,Main_login::class.java)
                        startActivity(intent)
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