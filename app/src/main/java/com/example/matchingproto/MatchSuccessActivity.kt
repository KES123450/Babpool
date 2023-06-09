package com.example.matchingproto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.matchingproto.databinding.ActivityMainBinding
import com.example.matchingproto.databinding.MatchSuccessBinding
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
import kotlin.properties.Delegates

class MatchSuccessActivity: AppCompatActivity() , OnMapReadyCallback {
    var myID = "김은서" // 임시로 설정해둔 내ID
    lateinit var mateID:String
    lateinit var matchBinding: MatchSuccessBinding
    var organizerCheck by Delegates.notNull<Boolean>() // 클라이언트가 파티의 주최자인지 참가자인지 판별
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    var mylatitude:Double =0.0
    var mylongitude:Double=0.0

    var matelatitude:Double =0.0
    var matelongitude:Double=0.0

    // 파베에서 데이터 불러올 주기(ms) 설정
    private val interval = 3000

    // Handler 객체 생성
    private val handler = Handler()

    var googleMap: GoogleMap? = null

    val userLocDB: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        matchBinding=MatchSuccessBinding.inflate(layoutInflater)
        mateID = intent.getStringExtra("mateName").toString()
        myID= intent.getStringExtra("myID").toString()
        organizerCheck= intent.getBooleanExtra("organizerCheck",false)

        matchBinding.nickname.text=mateID
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(10 * 1000) // 10 seconds
            .setFastestInterval(1 * 1000) // 1 second
        setContentView(matchBinding.root)
        (supportFragmentManager.findFragmentById(R.id.mapView) as
                SupportMapFragment?)!!.getMapAsync(this)
        getMateLoc()
        val runnable = object : Runnable {
            override fun run() {
                Log.d("log",mylatitude.toString())

                setUserLoc()

                googleMap?.clear()
                makeMarker(mylatitude,mylongitude,myID)
                makeMarker(matelatitude,matelongitude,mateID)

                // 일정 시간 간격으로 다시 실행
                handler.postDelayed(this, interval.toLong())
            }
        }
        handler.postDelayed(runnable, interval.toLong())


        // 주최자가맞다면 매칭종료버튼이 보이게
        if(organizerCheck==true){
            matchBinding.finish.visibility= View.VISIBLE
        }
        else{
            waitFinish()
        }

        matchBinding.chat.setOnClickListener {
            var intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        matchBinding.finish.setOnClickListener{
            finishMatching()
            Toast.makeText(this, "매칭 종료!", Toast.LENGTH_SHORT).show()
        }

        mylocationMoveMap()

    }

    private fun finishMatching(){
        val task1: Task<DocumentSnapshot> = userLocDB.collection("User_Info").document(myID).get()
        val task2: Task<DocumentSnapshot> = userLocDB.collection("User_Info").document(mateID).get()

        Tasks.whenAllSuccess<DocumentSnapshot>(task1, task2)
            .addOnSuccessListener { result ->
                // 모든 작업이 성공한 경우 결과 처리
                val mynn = result[0].getString("Nickname")
                val matenn = result[1].getString("Nickname")

                // 필드 값들을 활용하여 추가 작업 수행(기록 저장)
                val myhis: Map<String, Any> = hashMapOf<String?, String?>(matenn to matenn) as Map<String, Any>
                val matehis: Map<String, Any> = hashMapOf<String?, String?>(mynn to mynn) as Map<String, Any>

                val myHisDocRef = userLocDB.collection("User_rec").document(mynn.toString())
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

                val mateHisDocRef = userLocDB.collection("User_rec").document(matenn.toString())
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

        userLocDB.collection("User_Loc")
            .document(mateID)
            .update("finish_check",true)

        userLocDB.collection("User_Loc")
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

        val intent: Intent = Intent(this,Main_login::class.java)
        startActivity(intent)
    }


    private fun waitFinish() {
        userLocDB.collection("User_Loc")
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
                        userLocDB.collection("User_Loc")
                            .document(myID)
                            .delete()

                        Toast.makeText(this, "매칭 종료!", Toast.LENGTH_SHORT).show()
                        val intent: Intent = Intent(this,Main_login::class.java)
                        startActivity(intent)

                    }
                }
            }
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
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.lastLocation?.let {
                    mylatitude = it.latitude
                    mylongitude = it.longitude
                    // do something with latitude and longitude
                }
            }
        }, null)

    }

    private fun makeMarker(latitude:Double,longitude:Double,ID:String){
        //마커표시

        val markerOption= MarkerOptions()
        markerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker2))
        markerOption.position(LatLng(latitude, longitude))
        markerOption.title(ID)

        googleMap?.addMarker(markerOption)
    }

    private fun mylocationMoveMap(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // 위치 가져오기 성공
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    // 위치를 이용한 작업 수행
                    moveMap(latitude, longitude)
                } else {
                    // 위치 정보를 찾을 수 없음
                }
            }
            .addOnFailureListener { exception: Exception ->
                // 위치 가져오기 실패
                // 예외 처리
            }
    }
    private fun moveMap(latitude: Double, longitude: Double) {
        val latLng = LatLng(latitude, longitude)
        val position = CameraPosition.Builder()
            .target(latLng)
            .zoom(14f)
            .build()
        googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(position))
    }
    override fun onMapReady(p0: GoogleMap?) {
        googleMap = p0

    }
}