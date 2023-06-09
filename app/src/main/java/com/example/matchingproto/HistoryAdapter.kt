package com.example.matchingproto

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class HistoryAdapter(val HistoryList : ArrayList<HistoryData>, val context: Context) : RecyclerView.Adapter<HistoryAdapter.CustomViewHolder>() {

    private val userDB: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HistoryAdapter.CustomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_history, parent, false)
        return CustomViewHolder(view, context)
    }

    override fun onBindViewHolder(holder: HistoryAdapter.CustomViewHolder, position: Int) {
        val nn = HistoryList.get(position).nickname
        holder.nickname.text  = nn

        //임시 저장한 사용자 이메일 불러오기
        val sharedPreferences = context.getSharedPreferences("MyPrefs2", Context.MODE_PRIVATE)
        val mynick = sharedPreferences.getString("nickname", "").toString()

        holder.like.setOnClickListener {
            userDB.collection("User_Info")
                .whereEqualTo("Nickname", nn)
                .get()
                .addOnSuccessListener { documents ->
                    // 가져온 정보가 있을 경우
                    if (!documents.isEmpty) {
                        // 첫번째 문서 가져오기
                        val user = documents.first()
                        var tmp = user.getLong("Recommendation") ?: 0
                        val tmp2 = tmp + 1

                        val docRef = userDB.collection("User_Info").document(user.id)
                        docRef.update("Recommendation", tmp2)
                            .addOnSuccessListener {
                                // 업데이트 성공


                                userDB.collection("User_rec").document(mynick)
                                    .update(nn,FieldValue.delete())
                                    .addOnSuccessListener {
                                        // Firestore 문서 삭제 성공
                                        // 리사이클러 뷰 아이템 삭제
                                        HistoryList.removeAt(position)
                                        notifyItemRemoved(position)
                                        notifyItemRangeChanged(position, HistoryList.size)
                                    }
                                    .addOnFailureListener { exception ->
                                        // Firestore 문서 삭제 실패
                                    }
                            }
                            .addOnFailureListener { exception ->
                                // 업데이트 실패
                            }
                    }
                }
        }

        holder.dislike.setOnClickListener {
            userDB.collection("User_Info")
                .whereEqualTo("Nickname", nn)
                .get()
                .addOnSuccessListener { documents ->
                    // 가져온 정보가 있을 경우
                    if (!documents.isEmpty) {
                        // 첫번째 문서 가져오기
                        val user = documents.first()
                        var tmp = user.getLong("Recommendation") ?: 0
                        val tmp2 = tmp - 1

                        val docRef = userDB.collection("User_Info").document(user.id)
                        docRef.update("Recommendation", tmp2)
                            .addOnSuccessListener {
                                // 업데이트 성공
                                userDB.collection("User_rec").document(mynick)
                                    .update(nn,FieldValue.delete())
                                    .addOnSuccessListener {
                                        // Firestore 문서 삭제 성공
                                        // 리사이클러 뷰 아이템 삭제
                                        HistoryList.removeAt(position)
                                        notifyItemRemoved(position)
                                        notifyItemRangeChanged(position, HistoryList.size)
                                    }
                                    .addOnFailureListener { exception ->
                                        // Firestore 문서 삭제 실패
                                    }
                            }
                            .addOnFailureListener { exception ->
                                // 업데이트 실패
                            }
                    }
                }
        }
    }

    override fun getItemCount(): Int {
        return HistoryList.size
    }

    class CustomViewHolder(itemView: View, private val context: Context) : RecyclerView.ViewHolder(itemView) {
        val nickname = itemView.findViewById<TextView>(R.id.tv_nn)
        val like = itemView.findViewById<ImageButton>(R.id.imgb_like)
        val dislike = itemView.findViewById<ImageButton>(R.id.imgb_dislike)

        init {
            // 여기서 context를 활용할 수 있습니다.
        }
    }

}