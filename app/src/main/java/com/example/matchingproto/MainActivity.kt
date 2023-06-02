package com.example.matchingproto

import android.Manifest;
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.matchingproto.databinding.ActivityMainBinding
import com.example.matchingproto.databinding.ItemMainBinding
import com.example.matchingproto.databinding.TopMenuLayoutBinding
import com.example.matchingproto.databinding.WritePartyBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationRequest.create
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import android.os.Handler
import com.google.firebase.firestore.FieldValue


class MainActivity : AppCompatActivity(),GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,OnMapReadyCallback{
    var myID = "김은서" // 임시로 설정해둔 내ID

    // 파베에서 데이터 불러올 주기(ms) 설정
    private val interval = 5000 // 1초

    // Handler 객체 생성
    private val handler = Handler()
    lateinit var binding: ActivityMainBinding
    lateinit var writebinding:WritePartyBinding
    lateinit var topbinding:TopMenuLayoutBinding
    var googleMap: GoogleMap? = null
    lateinit var providerClinet : FusedLocationProviderClient
    lateinit var apiClient : GoogleApiClient

    val datas = mutableListOf<ListData>()
    var latitude:Double =0.0
    var longitude:Double=0.0

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    val partyDB: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //userid가져오기
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        myID = sharedPreferences.getString("email", "").toString()


        binding=ActivityMainBinding.inflate(layoutInflater)
        writebinding=WritePartyBinding.inflate(layoutInflater)
        topbinding= TopMenuLayoutBinding.inflate(layoutInflater)

        setContentView(binding.root)

        (supportFragmentManager.findFragmentById(R.id.mapView) as
                SupportMapFragment?)!!.getMapAsync(this)

        binding.backBtn.setOnClickListener {
            val intent = Intent(this@MainActivity, Main_login::class.java)
            startActivity(intent)
        }
        providerClinet=LocationServices.getFusedLocationProviderClient(this)
        apiClient=GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()
        if(ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) !==
            PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) !==
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_NETWORK_STATE) !==
                    PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_NETWORK_STATE),
                            100
                        )
                    }
            else{
                apiClient.connect()
        }
        //setContentView(listBinding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(10 * 1000) // 10 seconds
            .setFastestInterval(1 * 1000) // 1 second


        binding.recyclerView.layoutManager=LinearLayoutManager(this)
        binding.recyclerView.adapter=MyAdapter(datas,this)
        binding.recyclerView.addItemDecoration(MyDecoration(this))

        binding.write.setOnClickListener{
            setContentView(writebinding.root)
        }
        writebinding.makeParty.setOnClickListener{
            makeParty()
            setContentView(binding.root)
            (binding.recyclerView.adapter as MyAdapter).notifyDataSetChanged()
            Log.d("data : ",datas.toString())


        }
        
        //임시로 유저id불러오는 버튼 리스너
//        binding.idBtn.setOnClickListener{
//            myID = binding.userId.text.toString()
//        }

        // Runnable 객체 생성
        val runnable = object : Runnable {
            override fun run() {
                datas.clear()
                getPartyData()


                // 일정 시간 간격으로 다시 실행
                handler.postDelayed(this, interval.toLong())
            }
        }
        handler.postDelayed(runnable, interval.toLong())

        mylocationMoveMap()
    }

    //map 이동하는 메서드
    public fun moveMap(latitude: Double, longitude: Double, title:String){
        googleMap?.clear()
        val latLng = LatLng(latitude, longitude)
        val position = CameraPosition.Builder()
            .target(latLng)
            .zoom(14f)
            .build()
        googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(position))

        //마커표시
        val markerOption=MarkerOptions()
        markerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker2))
        markerOption.position(latLng)
        markerOption.title(title)

        googleMap?.addMarker(markerOption)
    }

    public fun testIntent(){
        val intent:Intent = Intent(this,MatchSuccessActivity::class.java)
        startActivity(intent)

    }
    private fun makeParty(){
        val title=writebinding.partyTitle.text.toString()
        val body=writebinding.partyBody.text.toString()

        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val requestCode = 100

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            fetchLocation()
        }
        //datas.add(ListData(title,body,latitude,longitude))
        addPartyData(title, body, latitude, longitude)


    }

    private fun addPartyData(title: String, body:String, latitude: Double, longitude: Double){
        val partyData = mapOf(
            "title" to title,
            "body" to body,
            "latitude" to latitude,
            "longitude" to longitude,
            "organizerID" to myID,
            "participantID" to "tmp",
            "participate_check" to false,
            "current_time" to FieldValue.serverTimestamp()
        )

        val colRef:CollectionReference= partyDB.collection("party")
        val docRef:Task<DocumentReference> = colRef.add(partyData)
        docRef.addOnSuccessListener { documentReference ->
            val documentId = documentReference.id
            val intent:Intent = Intent(this,WaitParticipateActivity::class.java)
            intent.putExtra("partyID",documentId)
            intent.putExtra("myID",myID)
            startActivity(intent)
        }
        docRef.addOnFailureListener{e ->
            Log.w("log","에러",e)
        }

    }

    //party 컬렉션의 모든 문서 데이터를 가져와서 datas 에 업데이트하는 함수
    private fun getPartyData(){
        val partyRef=partyDB.collection("party")
        partyRef.get()
            .addOnSuccessListener { querySnapshot->
                for(document in querySnapshot.documents){
                    if(document.id.equals("TESTTMPPARTY")){
                        continue
                    }
                    if(document!=null){
                        val partyID = document.id.toString()
                        val title= document.getString("title").toString()
                        val body = document.getString("body").toString()
                        val latitude:Double = document.getDouble("latitude") ?:0.0
                        val longitude:Double = document.getDouble("longitude") ?:0.0

                        datas.add(ListData(partyID,title,body,latitude,longitude))

                    }
                    else{
                        Log.d("log","실패")
                    }
                }

            }
            .addOnFailureListener{exception->
                Log.d("log","연결실패")
            }
            .addOnCompleteListener{
                (binding.recyclerView.adapter as MyAdapter).notifyDataSetChanged()
            }

    }

    // 참가자가 참가할 파티의 참가 버튼을 누르면 실행되는 함수
    // party의 participate_check를 true로 바꾸고 participantID에 자기 ID를 넣는다
    public fun setParticipate(partyID:String){
        lateinit var mateName:String

        partyDB.collection("party")
            .document(partyID)
            .get()
            .addOnSuccessListener { document ->
                if(document !=null){
                    mateName=document.getString("organizerID").toString()
                    Log.d("log",mateName)
                    Log.d("log",partyID)
                    partyDB.collection("User_Loc")
                        .document(myID)
                        .set(
                            mapOf(
                                "longitude" to longitude,
                                "latitude" to latitude,
                                "finish_check" to false
                            )
                        )

                }
            }
            .addOnFailureListener { exception ->
                Log.d("log", "Error getting document: $exception")}


        partyDB.collection("party")
            .document(partyID)
            .update(mapOf(
                "participate_check" to true,
                "participantID" to myID

            ))
            .addOnSuccessListener {
                val intent:Intent = Intent(this,MatchSuccessActivity::class.java)
                intent.putExtra("mateName",mateName)
                intent.putExtra("myID",myID)
                intent.putExtra("organizerCheck",false)

                startActivity(intent)

            }
    }



    override fun onMapReady(p0: GoogleMap?) {
        googleMap = p0

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
                    latitude = it.latitude
                    longitude = it.longitude
                    // do something with latitude and longitude
                }
            }
        }, null)
    }


    override fun onConnected(p0: Bundle?) {
        if(ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION) ===
                    PackageManager.PERMISSION_GRANTED)
        {
            providerClinet.getLastLocation().addOnSuccessListener (
                this@MainActivity,
                object :OnSuccessListener<Location>{
                    override fun onSuccess(location: Location?) {
                        location?.let{
                            latitude = location.latitude
                            longitude = location.longitude
                        }
                    }


            })
            apiClient.disconnect()
        }

    }
    override fun onConnectionSuspended(p0: Int){

    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        TODO("Not yet implemented")
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation()
            }
        }
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
                    FirstMoveMap(latitude, longitude)
                } else {
                    // 위치 정보를 찾을 수 없음
                }
            }
            .addOnFailureListener { exception: Exception ->
                // 위치 가져오기 실패
                // 예외 처리
            }
    }
    private fun FirstMoveMap(latitude: Double, longitude: Double) {
        val latLng = LatLng(latitude, longitude)
        val position = CameraPosition.Builder()
            .target(latLng)
            .zoom(14f)
            .build()
        googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(position))
    }
    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

}