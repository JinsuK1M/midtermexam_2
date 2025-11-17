package com.example.photoviewer

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout




class MainActivity : AppCompatActivity() {
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ImageAdapter

    private val siteUrl = "http://192.168.0.12:8000"

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) uploadImage(uri)
        else Toast.makeText(this, "선택 취소", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        swipe = findViewById(R.id.swipe)
        swipe.setOnRefreshListener {
            // 아래로 당겨 새로고침
            loadList()
        }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ImageAdapter()
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btnSync).setOnClickListener { onClickDownload(it) }
        findViewById<Button>(R.id.btnUpload).setOnClickListener { onClickUpload(it) }
    }

    fun onClickDownload(@Suppress("UNUSED_PARAMETER") v: View) {
        loadList()
    }

    fun onClickUpload(@Suppress("UNUSED_PARAMETER") v: View) {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun uploadImage(uri: Uri) {
        Thread {
            try {
                val contentType = contentResolver.getType(uri)?.toMediaTypeOrNull()
                    ?: "image/*".toMediaTypeOrNull()
                val fileName = getDisplayName(uri) ?: "upload.jpg"
                val bytes = readAllBytes(uri)

                val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("title", "android upload")
                    .addFormDataPart("author", "JinsuKIM")
                    .addFormDataPart("image", fileName, bytes.toRequestBody(contentType))
                    .build()

                val client = OkHttpClient.Builder()
                    .callTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                val req = Request.Builder()
                    .url("$siteUrl/api_root/Post/")
                    .post(body)
                    .build()

                val resp = client.newCall(req).execute()
                val ok = resp.isSuccessful
                val msg = resp.body?.string()
                resp.close()

                runOnUiThread {
                    if (ok) {
                        Toast.makeText(this, "업로드 성공", Toast.LENGTH_SHORT).show()
                        loadList()
                    } else {
                        Toast.makeText(this, "업로드 실패: $msg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "업로드 오류: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun loadList() {
        Thread {
            try {
                val url = URL("$siteUrl/api_root/Post/")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                val code = conn.responseCode
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val items = mutableListOf<Photo>()
                if (code == 200) {
                    val arr = JSONArray(body)
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val id = o.optInt("id")
                        val title = o.optString("title")
                        val author = o.optString("author")
                        val imagePath = o.optString("image")
                        val createdAt = o.optString("created_at")
                        val full = if (imagePath.startsWith("http")) imagePath else "$siteUrl$imagePath"
                        items.add(Photo(id, title, author, full, createdAt))
                    }
                }

                runOnUiThread {
                    adapter.submitList(items)
                    if (items.isEmpty()) {
                        Toast.makeText(this, "서버 OK, 데이터 0건", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "목록 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun readAllBytes(uri: Uri): ByteArray =
        contentResolver.openInputStream(uri)!!.use { it.readBytes() }

    private fun getDisplayName(uri: Uri): String? {
        val c = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        c?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return it.getString(idx)
            }
        }
        return null
    }
}