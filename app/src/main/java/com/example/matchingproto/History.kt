package com.example.matchingproto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class History : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)


        //임시 저장한 사용자 이메일 불러오기
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val emailAddr = sharedPreferences.getString("email", "").toString()

        val backbtn = findViewById<ImageButton>(R.id.backBtn)
        backbtn.setOnClickListener {
            val intent = Intent(this, Mypage::class.java)
            startActivity(intent)
            finish()
        }

        val historylist = arrayListOf<HistoryData>()

        // Firestore 데이터 가져오기
        //임시 저장한 사용자 닉네임 불러오기
        val sharedPreferences2 = getSharedPreferences("MyPrefs2", Context.MODE_PRIVATE)
        val Nickname = sharedPreferences2.getString("nickname", "").toString()
        val documentRef = FirebaseFirestore.getInstance().collection("User_rec").document(Nickname)

        documentRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val dataMap = documentSnapshot.data // 문서의 모든 필드와 값을 포함하는 맵
                if(dataMap != null) {
                    for ((fieldName, value) in dataMap) {
                        // 필드 이름과 해당 필드의 값 출력
                        val historyData = HistoryData(value.toString()) // 필요한 데이터 필드에 맞게 처리
                        historylist.add(historyData)

                        // RecyclerView에 데이터 설정
                        val rv_his = findViewById<RecyclerView>(R.id.rv_history)
                        rv_his.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
                        rv_his.setHasFixedSize(true)
                        rv_his.adapter = HistoryAdapter(historylist,this)
                    }
                } else {
                    // 문서가 존재하지 않는 경우 처리
                }
            }
        }.addOnFailureListener { e ->
            // 오류 처리
            Log.e("Firestore", "Error getting document: $e")
        }

    }

    override fun onBackPressed() {
        val intent = Intent(this, Mypage::class.java)
        startActivity(intent)
        finish()
    }

}
