package com.fbiego.dt78.app

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.fbiego.dt78.R
import kotlinx.android.synthetic.main.activity_error_log.*
import java.io.File

class ErrorLogActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error_log)

        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)




    }

    override fun onResume() {
        super.onResume()
        val directory = this.cacheDir

        val file = File(directory, "error.txt")

        var error = ""

        error = if (!file.exists()){
            getString(R.string.no_errors)
        } else {
            if (file.canRead()){

                file.readText(Charsets.UTF_8)
            } else {
                getString(R.string.unable_load)
            }
        }

        errText.text = error
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.error_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete -> {
                val cachePath = this.cacheDir
                val file = File(cachePath, "error.txt")
                if (file.exists()){
                    file.delete()
                    Toast.makeText(this, R.string.log_deleted, Toast.LENGTH_SHORT).show()
                    errText.text = getString(R.string.log_deleted)

                } else {
                    Toast.makeText(this, R.string.nothing, Toast.LENGTH_SHORT).show()
                }

                true
            }
            R.id.share -> {
                val cachePath = this.cacheDir
                val file = File(cachePath, "error.txt")

                if (file.exists()){
                    val contentUri = FileProvider.getUriForFile(this, "com.fbiego.dt78.fileprovider", file)


                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "*/*"
                    intent.putExtra(Intent.EXTRA_STREAM, contentUri)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.putExtra(Intent.EXTRA_TEXT, "Share error log")
                    intent.putExtra(Intent.EXTRA_SUBJECT,"Send")
                    startActivity(Intent.createChooser(intent, "Share"))
                } else {
                    Toast.makeText(this, R.string.no_errors, Toast.LENGTH_SHORT).show()
                }


                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}