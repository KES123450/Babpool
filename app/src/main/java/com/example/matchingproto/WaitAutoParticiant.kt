package com.example.matchingproto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.matchingproto.databinding.ActivitySucceedMatchBinding
import com.example.matchingproto.databinding.WaitAutoMatchingBinding
import com.google.firebase.firestore.FirebaseFirestore

class WaitAutoParticiant: AppCompatActivity()  {
    val DB: FirebaseFirestore = FirebaseFirestore.getInstance()
    lateinit var myID:String
    lateinit var waitBinding: WaitAutoMatchingBinding
    // 파베에서 데이터 불러올 주기(ms) 설정
    private val interval = 10000

    // Handler 객체 생성
    private val handler = Handler()
    val runnable = object : Runnable {
        override fun run() {
            // 일정 시간 간격으로 다시 실행
            findParticipant()
            handler.postDelayed(this, interval.toLong())

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        waitBinding= WaitAutoMatchingBinding.inflate(layoutInflater)

        //userid가져오기
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        myID = sharedPreferences.getString("email", "").toString()
        setContentView(waitBinding.root)
        waitParticipant()


        handler.postDelayed(runnable, interval.toLong())
    }

    private fun waitParticipant(){
        DB.collection("auto")
        .document(myID)
        .addSnapshotListener { documentSnapshot, e ->
            if (e != null) {
                Log.d("Firestore", "Listen failed: $e")
                return@addSnapshotListener
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                val documentData = documentSnapshot.data


                val participateFind = documentData?.get("participate_find") as? Boolean
                if (participateFind != null) {
                    // participateCheck 사용하는 코드
                    // ...
                    if(participateFind==true){
                        val participantID = documentData?.get("participantID").toString()

                        val intent: Intent = Intent(this,FoundMatchActivity::class.java)
                        intent.putExtra("mateID",participantID)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    // participateCheck가 null인 경우 처리
                    // ...
                }



            } else {
                Log.d("Firestore", "Document does not exist")
            }
        }

    }

    private fun findParticipant(){
        DB.collection("auto")
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    // 각각의 문서(document)에 대한 처리
                    val documentID = document.id

                    if(documentID==myID){
                        continue
                    }

                    if(documentID=="AUTOTMP"){
                        continue
                    }

                    DB.collection("auto")
                        .document(documentID)
                        .update(mapOf("participate_find" to true,
                        "participantID" to myID))
                        .addOnSuccessListener {
                            DB.collection("auto")
                                .document(myID)
                                .update(mapOf("participate_find" to true,
                                "participantID" to documentID))
                                .addOnSuccessListener {
                                    val intent: Intent = Intent(this,FoundMatchActivity::class.java)
                                    intent.putExtra("mateID",documentID)
                                    startActivity(intent)
                                    finish()
                                }


                        }
                }
            }

    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(runnable)
    }

}