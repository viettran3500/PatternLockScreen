package com.viet.patternlockscreen

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PointF
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sharedPreferences = getSharedPreferences("Password", Context.MODE_PRIVATE)

        val data = sharedPreferences.getString("PASS", "")
        val intent = Intent(this, MainActivity::class.java)
        if (data!!.isEmpty()) {
            startActivity(intent)
            finish()
        }

        lockView.setOnDrawCompleteListener(object : LockView.OnDrawCompleteListener {
            override fun onComplete(selectedList: MutableList<PointF>) {
                if (data == selectedList.toString()) {
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Wrong Password", Toast.LENGTH_SHORT).show()
                }
            }

        })
    }
}