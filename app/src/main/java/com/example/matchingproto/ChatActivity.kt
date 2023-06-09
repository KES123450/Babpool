package com.example.matchingproto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ChatActivity : AppCompatActivity() {

    private var  db = Firebase.firestore

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: RecyclerView.Adapter<*>
    private lateinit var mLayoutManager: RecyclerView.LayoutManager
    private lateinit var chatList: MutableList<ChatData>

    private lateinit var EditText_chat: EditText
    private lateinit var Button_send: ImageButton
    private var  database = FirebaseDatabase.getInstance()
    private var myRef : DatabaseReference = database.reference.child("chatting")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        var backbtn = findViewById<ImageButton>(R.id.backBtn)

        backbtn.setOnClickListener {
            onBackPressed()
        }

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val emailAddr = sharedPreferences.getString("email", "")
        val docref = db.collection("User_Info").document(emailAddr.toString())

        Button_send = findViewById(R.id.btn_send)
        EditText_chat = findViewById(R.id.et_chat)
        docref.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val nick = documentSnapshot.getString("Nickname")
                    // nickname 값 사용
                    if (nick != null) {
                        Button_send.setOnClickListener {
                            val msg = EditText_chat.text.toString()
                            EditText_chat.text.clear()
                            if (msg.isNotEmpty()) {
                                val chat = ChatData().apply {
                                    nickname = nick
                                    this.msg = msg
                                }
                                myRef.push().setValue(chat)
                            }
                        }

                        mRecyclerView = findViewById(R.id.rcv_chat)
                        mRecyclerView.setHasFixedSize(true)
                        mLayoutManager = LinearLayoutManager(this)
                        mRecyclerView.layoutManager = mLayoutManager

                        chatList = ArrayList()
                        mAdapter = ChatAdapter(chatList, this, nick)
                        mRecyclerView.adapter = mAdapter

                        val database = FirebaseDatabase.getInstance()
                        myRef = database.reference.child("chatting")

                        myRef.addChildEventListener(object : ChildEventListener {
                            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                                Log.d("CHATCHAT", dataSnapshot.value.toString())
                                val chat = dataSnapshot.getValue(ChatData::class.java)
                                if(chat != null) {
                                    (mAdapter as ChatAdapter).addChat(chat)
                                }
                            }

                            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}

                            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}

                            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}

                            override fun onCancelled(databaseError: DatabaseError) {}
                        })
                        // 필요한 로직 처리
                    }
                } else {
                    // 문서가 존재하지 않을 때의 처리
                }
            }
            .addOnFailureListener { exception ->
                // 오류 처리
            }

    }
}
