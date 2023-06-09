package com.example.matchingproto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Mypage : ComponentActivity() {
    private val TAG = "Mypage"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mypage)

        val db = Firebase.firestore     //firebase, firestore 연동

        var backBtn = findViewById<ImageView>(R.id.backBtn)
        var noticeBtn = findViewById<Button>(R.id.notice_btn)

        var nn = findViewById<TextView>(R.id.tv_nickname)
        var age = findViewById<TextView>(R.id.tv_age)
        var gender = findViewById<TextView>(R.id.tv_gender)
        var rec = findViewById<TextView>(R.id.tv_recommendation)

        //임시 저장한 사용자 이메일 불러오기
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val emailAddr = sharedPreferences.getString("email", "")

        // 특정 필드값을 가지는 사람의 정보 가져오기(ex. ID = 0008)
        db.collection("User_Info")
            .whereEqualTo("ID", emailAddr)
            .get()
            .addOnSuccessListener { documents ->
                // 가져온 정보가 있을 경우
                if (!documents.isEmpty) {
                    // 첫번째 문서 가져오기
                    val user = documents.first()

                    // 정보를 TextView에 띄우기
                    age.text = user.getLong("Age").toString()
                    gender.text = user.getString("Gender")
                    nn.text = user.getString("Nickname")
                    rec.text = user.getLong("Recommendation").toString()

                    //사용자 닉네임 임시저장
                    val sharedPreferences2 = getSharedPreferences("MyPrefs2", Context.MODE_PRIVATE)
                    val editor = sharedPreferences2.edit()
                    editor.putString("nickname", user.getString("Nickname"))
                    editor.apply()
                }
            }
            .addOnFailureListener { exception ->
                // 가져오기 실패시 처리할 작업
                Log.w(TAG, "Error getting documents: ", exception)
            }


        //공지사항 이동
        noticeBtn.setOnClickListener {
            intent = Intent(this, Notice :: class.java)
            startActivity(intent)
            finish()
        }

        var history = findViewById<Button>(R.id.btn_history)

        history.setOnClickListener {
            intent = Intent(this, History::class.java)
            startActivity(intent)
            finish()
        }

        //뒤로가기(메인페이지)
        backBtn.setOnClickListener {
            intent = Intent(this, Main_login :: class.java)
            startActivity(intent)
            finish()
        }

    }
}