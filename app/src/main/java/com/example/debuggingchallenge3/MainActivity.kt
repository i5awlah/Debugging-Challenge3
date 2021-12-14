package com.example.debuggingchallenge3

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL


class MainActivity : AppCompatActivity() {
    private val definitions = arrayListOf<ArrayList<String>>()

    private lateinit var rvMain: RecyclerView
    private lateinit var rvAdapter: RVAdapter
    private lateinit var etWord: EditText
    private lateinit var btSearch: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvMain = findViewById(R.id.rvMain)
        rvAdapter = RVAdapter(definitions)
        rvMain.adapter = rvAdapter
        rvMain.layoutManager = LinearLayoutManager(this)

        etWord = findViewById(R.id.etWord)
        btSearch = findViewById(R.id.btSearch)
        btSearch.setOnClickListener {
            // (4) the app crashes if there is no internet connection
            if (checkConnection()) {
                requestAPI()
            } else {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkConnection() : Boolean {
        val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    private fun requestAPI(){
        if(etWord.text.isNotEmpty()){
            CoroutineScope(IO).launch {
                val data = async{
                    getDefinition(etWord.text.toString())
                }.await()
                if(data.isNotEmpty()){
                    updateRV(data)
                } else {
                    withContext(Main) {
                        Toast.makeText(this@MainActivity, "Unable to get data", Toast.LENGTH_LONG)
                            .show() // Solution 5
                    }
                }
            }
        }else{
            // (3) the Toast message never appears
            //Toast.makeText(this, "Please enter a word", Toast.LENGTH_LONG)
            Toast.makeText(this, "Please enter a word", Toast.LENGTH_LONG).show()
        }
    }

    private fun getDefinition(word: String): String{
        var response = ""
        try {
            // (1) the API always thinks the user has entered the word 'house'
            //response = URL("https://api.dictionaryapi.dev/api/v2/entries/en/house").readText(Charsets.UTF_8)
            response = URL("https://api.dictionaryapi.dev/api/v2/entries/en/$word").readText(Charsets.UTF_8)
        }catch (e: Exception){
            println("Error: $e")
            // (5) the app crashes when an invalid word is entered
            //Toast.makeText(this, "Unable to get data", Toast.LENGTH_LONG).show()
        }
        return response
    }

    private suspend fun updateRV(result: String){
        withContext(Main){
            Log.d("MAIN", "DATA: $result")

            val jsonArray = JSONArray(result)
            val main = jsonArray[0]
            val word = JSONObject(main.toString()).getString("word")

            val inside = jsonArray.getJSONObject(0).getJSONArray("meanings")
                .getJSONObject(0)
            // (2) the definition value is wrong
            //val definition = JSONObject(inside.toString()).getString("definition")
            val definition = JSONObject(inside.toString()).getJSONArray("definitions").getJSONObject(0).getString("definition")
            Log.d("MAIN", "WORD: $word $definition")
            definitions.add(arrayListOf(word, definition))
            rvAdapter.update()
            etWord.text.clear()
            etWord.clearFocus()
            rvMain.scrollToPosition(definitions.size - 1)
        }
    }
}