package com.gusakov.internetinandroid

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.gusakov.library.internet.InternetModule
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val internetModule = InternetModule.Builder(this).build()
        tv_internet_info.movementMethod = ScrollingMovementMethod()
        internetModule.startListening(this) {
            if (it) {
                tv_internet_info.append("\ninternet is connected to world")
                Log.e("test","internet is connected to world")
            } else {
                tv_internet_info.append("\ninternet is not connected to world")
                Log.e("test","internet is not connected to world")

            }
        }
        btn_test_internet_speed.setOnClickListener {
            tv_internet_info.append("\nstart internet speed testing")
            internetModule.getInternetSpeed { unitPerSecond, unit ->
                tv_internet_info.append("\ninternet speed is $unitPerSecond ${unit.name}/s")
            }
        }
    }
}
