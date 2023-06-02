package com.example.matchingproto

class ChatData {
    var msg: String? = null
    var nickname: String? = null

    fun getMessage(): String? { return msg }
    fun setMessage(msg: String?) { this.msg = msg }
    fun getNn(): String? { return nickname }
    fun setNn(nickname: String?) { this.nickname = nickname }
}