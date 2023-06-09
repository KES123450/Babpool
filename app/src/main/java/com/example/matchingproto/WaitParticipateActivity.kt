package com.example.matchingproto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.matchingproto.databinding.WaitParticipantsBinding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class WaitParticipateActivity : AppCompatActivity(), OnMapReadyCallback {
    lateinit var binding: WaitParticipantsBinding
    lateinit var partyID:String
    val partyDB: FirebaseFirestore = FirebaseFirestore.getInstance()
    var myID:String="test"
    var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        partyID = intent.getStringExtra("partyID").toString()
        myID=intent.getStringExtra("myID").toString()
        binding=WaitParticipantsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imgbBack.setOnClickListener {
            showExitConfirmationDialog()
        }
        waitParticipant()
        (supportFragmentManager.findFragmentById(R.id.mapView) as
                SupportMapFragment?)!!.getMapAsync(this)
    }

    private fun waitParticipant(){
        partyDB.collection("party")
            .document(partyID)
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Log.d("Firestore", "Listen failed: $e")
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val documentData = documentSnapshot.data


                    val participateCheck = documentData?.get("participate_check") as? Boolean
                    if (participateCheck != null) {
                        // participateCheck 사용하는 코드
                        // ...
                    } else {
                        // participateCheck가 null인 경우 처리
                        // ...
                    }

                    if(participateCheck == true){
                        val participantID = documentData?.get("participantID") as? String
                        if (participantID != null) {
                            // participantID 사용하는 코드
                            // ...
                        } else {
                            // participantID가 null인 경우 처리
                            // ...
                        }

                        partyDB.collection("party")
                            .document(partyID)
                            .delete()

                        partyDB.collection("User_Loc")
                            .document(myID)
                            .set(
                                mapOf(
                                    "longitude" to 0.0,
                                    "latitude" to 0.0,
                                    "finish_check" to false
                                )
                            )



                        val intent: Intent = Intent(this,MatchSuccessActivity::class.java)
                        intent.putExtra("mateName",participantID)
                        intent.putExtra("myID",myID)
                        intent.putExtra("organizerCheck",true)
                        startActivity(intent)

                    }

                } else {
                    Log.d("Firestore", "Document does not exist")
                }
            }
    }

    //매칭 취소 여부 확인 창
    private fun showExitConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("매칭 종료 확인")
            .setMessage("매칭을 취소하시겠습니까?")
            .setPositiveButton("예") { dialog, which ->
                // '예' 버튼을 클릭한 경우 종료 로직을 구현
                val task1 = partyDB.collection("party")
                    .whereEqualTo("organizerID",myID)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            document.reference.delete()
                                .addOnSuccessListener {
                                    // 문서 삭제 성공
                                }
                                .addOnFailureListener { exception ->
                                    // 문서 삭제 실패
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        // 문서 가져오기 실패
                    }

                val intent = Intent(this, Main_login::class.java)
                startActivity(intent)
                finish()
                Toast.makeText(this, "매칭을 취소합니다.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("아니오") { dialog, which ->
                // '아니오' 버튼을 클릭한 경우 아무 동작 없음
            }
            .show()
    }

    override fun onMapReady(p0: GoogleMap?) {
        googleMap = p0

    }
}
