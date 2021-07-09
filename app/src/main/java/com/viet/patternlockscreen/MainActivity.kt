package com.viet.patternlockscreen

import android.content.Context
import android.content.SharedPreferences
import android.graphics.PointF
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initPreferences()
        val pass = sharedPreferences.getString("PASS", "")
        if (pass!!.isEmpty()) {
            btnChange.visibility = View.INVISIBLE
            btnCreate.visibility = View.VISIBLE
        } else {
            btnChange.visibility = View.VISIBLE
            btnCreate.visibility = View.INVISIBLE
        }

        lockViewCreate.setOnDrawCompleteListener(object : LockView.OnDrawCompleteListener {

            override fun onComplete(selectedList: MutableList<PointF>) {
                editor.putString("PASS", selectedList.toString())
                editor.commit()
                Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_SHORT).show()
                lockViewCreate.visibility = View.INVISIBLE
                btnChange.visibility = View.VISIBLE
                btnCreate.visibility = View.INVISIBLE
                textViewTitle.text = "Welcome"
            }

        })

        lockViewChange.setOnDrawCompleteListener(object : LockView.OnDrawCompleteListener {

            override fun onComplete(selectedList: MutableList<PointF>) {
                val pas = sharedPreferences.getString("PASS", "")
                if (pas != selectedList.toString()) {
                    lockViewChange.setColorError()
                    Toast.makeText(this@MainActivity, "Wrong Password", Toast.LENGTH_SHORT).show()
                } else {
                    textViewTitle.text = "Enter new password"
                    lockViewCreate.visibility = View.VISIBLE
                    lockViewChange.visibility = View.INVISIBLE
                }
            }
        })

        btnCreate.setOnClickListener {
            textViewTitle.text = "Enter new password"
            lockViewCreate.visibility = View.VISIBLE
        }

        btnChange.setOnClickListener {
            textViewTitle.text = "enter old password"
            lockViewChange.visibility = View.VISIBLE
        }

    }


    private fun initPreferences() {
        sharedPreferences = getSharedPreferences("Password", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
    }
}