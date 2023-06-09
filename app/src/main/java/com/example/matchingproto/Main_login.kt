package com.example.matchingproto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class Main_login : ComponentActivity() {

    val partyDB: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var recent:TextView
    private lateinit var recentTime:TextView
    private var backPressedTime: Long = 0
    private val backPressThreshold: Long = 2000 // 두 번 눌러야 하는 시간 간격 (2초)
    private val TAG = "Main_login"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_login)

        recent = findViewById<TextView>(R.id.recent_party)
        var userBtn = findViewById<ImageButton>(R.id.user)

        userBtn.setOnClickListener {
            val intent = Intent(this, Mypage::class.java)
            startActivity(intent)
        }

        var partyBtn = findViewById<Button>(R.id.party_btn)
        partyBtn.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        var autoBtn = findViewById<Button>(R.id.auto_btn)
        autoBtn.setOnClickListener{
            val intent = Intent(this, AutoMatchingActivity::class.java)
            startActivity(intent)
        }
        getRecentParty()

    }
    override fun onBackPressed() {
        if (System.currentTimeMillis() - backPressedTime < backPressThreshold) {
            super.onBackPressed()
            finishAffinity()
            System.exit(0)
        } else {
            Toast.makeText(this, "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
            backPressedTime = System.currentTimeMillis()
        }
    }

    private fun getRecentParty(){
        partyDB.collection("party")
            .orderBy("current_time", Query.Direction.DESCENDING)
            .addSnapshotListener{querySnapshot, e ->
                if (e != null) {
                    // 데이터 가져오기 실패시의 처리
                    return@addSnapshotListener
                }


                if (querySnapshot!!.documents.isNotEmpty()) {

                    Log.d("log", querySnapshot.documents.toString())
                    val firstDocument = querySnapshot.documents[0]
                    val title = firstDocument.getString("title")
                    /*val partyTimeStamp =firstDocument.getString("current_time")*/

                   /* val partyDate: Date? = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(partyTimeStamp)
                    val nowTime=FieldValue.serverTimestamp()
                    val calendar = Calendar.getInstance()
                    calendar.time = partyDate
                    val timeDifference = ( calendar[Calendar.SECOND]*1000 - nowTime) / (1000 * 60)
                    recentTime.text=timeDifference.toString()
                    val partyDate: Date? = SimpleDateFormat("mm").parse(partyTimeStamp)
                    recentTime.text=partyDate.toString()*/
                    recent.text=title

                }
                else{
                    recent.text="최근 파티 없음"
                }
            }
    }
}