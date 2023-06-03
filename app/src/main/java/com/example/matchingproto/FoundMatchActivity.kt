package com.example.matchingproto

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.matchingproto.databinding.ActivityAutoMatchingBinding
import com.example.matchingproto.databinding.ActivityFoundMatchBinding
import com.google.firebase.firestore.FirebaseFirestore

class FoundMatchActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityFoundMatchBinding
    private lateinit var mateID:String
    private lateinit var myID:String

    private var myAcceptance = false
    private var mateAcceptance = false

    val DB: FirebaseFirestore = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_found_match)
        mateID=intent.getStringExtra("mateID").toString()
        // Activity Binding 객체에 할당 및 View 설정
        mBinding = ActivityFoundMatchBinding.inflate(layoutInflater)
        val view = mBinding.root
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        myID = sharedPreferences.getString("email", "").toString()

        setContentView(view)

        findMateData()
        waitRefuse()
        mBinding.yesBtn.setOnClickListener {

            // yes 응답 서버로 제출 -> 상대방도 yes를 누를 때까지 대기
            acceptMate()
        }
        mBinding.noBtn.setOnClickListener {
            // 근데 상대가 no를 눌렀을 때 자동으로 매칭 페이지로 돌아가야 함
            refuseMate()
        }
    }

    private fun findMateData(){
        DB.collection("User_Info")
            .document(mateID)
            .get()
            .addOnSuccessListener {document ->
                if (document.exists()) {
                    val nickname = document.getString("Nickname")
                    val age = document.getDouble("Age")
                    val gender = document.getString("Gender")
                    val recommendation = document.getDouble("Recommendation")

                    mBinding.nickName.text=nickname
                    mBinding.age.text =age.toString()
                    mBinding.sex.text =gender
                    mBinding.thumbs.text =recommendation.toString()
                } else {

                }
            }
    }

    private fun acceptMate() {
        DB.collection("auto")
            .document(mateID)
            .update("matchingCheck", "ACCEPT")
            .addOnSuccessListener {
                Toast.makeText(getApplicationContext(), "상대의 대답을 기다리고 있습니다.", Toast.LENGTH_LONG).show();

                // 내 ACCEPT 상태를 저장
                myAcceptance = true

                // 상대방의 상태와 비교하여 처리
                checkBothAccepted()
            }

        // mateAcceptance 업데이트
        DB.collection("auto")
            .document(myID)
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Log.d("Firestore", "Listen failed: $e")
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val matchingCheck = documentSnapshot.getString("matchingCheck")
                    if (matchingCheck == "ACCEPT") {
                        // 상대방의 ACCEPT 상태를 저장
                        mateAcceptance = true

                        // 상대방의 상태와 비교하여 처리
                        checkBothAccepted()
                    }
                }
            }
    }

    // checkBothAccepted 메서드 수정
    private fun checkBothAccepted() {
        if (myAcceptance && mateAcceptance) {
            // 초기화
            myAcceptance = false
            mateAcceptance = false

            // 두 사용자가 모두 ACCEPT 상태일 때 처리하는 로직을 추가
            DB.collection("User_Loc")
                .document(myID)
                .set(
                    mapOf(
                        "longitude" to 0.0,
                        "latitude" to 0.0,
                        "finish_check" to false
                    )
                )
            DB.collection("auto")
                .document(myID)
                .delete()

            val intent = Intent(this@FoundMatchActivity, SucceedMatchActivity::class.java)
            intent.putExtra("mateID", mateID)
            intent.putExtra("myID", myID)
            startActivity(intent)
            finish()
        }
    }

    private fun handleFailure(e: Exception) {
        Log.d("Firestore", "Error: $e")
        // 오류 처리 로직 추가
    }

    private fun refuseMate(){
        DB.collection("auto")
            .document(mateID)
            .update("matchingCheck", "REFUSE")

        DB.collection("auto")
            .document(myID)
            .delete()

        val intent = Intent(this@FoundMatchActivity, AutoMatchingActivity::class.java)
        startActivity(intent)
        finish()
        Toast.makeText(getApplicationContext(), "매칭에 실패하였습니다.", Toast.LENGTH_LONG).show();
    }

    //상대가 거부했을 시 바로 초기화면으로
    private fun waitRefuse(){
        DB.collection("auto")
            .document(myID)
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Log.d("Firestore", "Listen failed: $e")
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val matchingCheck= documentSnapshot.getString("matchingCheck")

                    if(matchingCheck.equals("REFUSE")){

                        DB.collection("auto")
                            .document(myID)
                            .delete()

                        val intent = Intent(this@FoundMatchActivity, AutoMatchingActivity::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(getApplicationContext(), "상대의 거절로 매칭에 실패하였습니다.", Toast.LENGTH_LONG).show();
                    }
                }
            }
    }
}