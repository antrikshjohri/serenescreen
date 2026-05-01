package app.serenescreen.helper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import app.serenescreen.R

class FakeHomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fake_home)
    }
}