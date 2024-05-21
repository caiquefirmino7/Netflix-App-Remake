package com.caique.netflixappremake.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.caique.netflixappremake.model.Category
import com.caique.netflixappremake.model.Movie
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection
import javax.security.auth.callback.Callback

class CategoryTask(private val callback: Callback) {

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    interface Callback {
        fun onPreExecute()
        fun onResult(categories: List<Category>)
        fun onFailure(message: String)
    }

    fun execute(url: String) {
        callback.onPreExecute()
        // utilizando a UI-thread(1)


        executor.execute {
            var urlConnection: HttpsURLConnection? = null
            val inputStream: InputStream?

            try {
                // utilizando a NOVA-thread (processo paralelo)
                val requestURL = URL(url) // abrir uma URL
                urlConnection = requestURL.openConnection() as HttpsURLConnection // abrir conexão com servidor
                urlConnection.readTimeout = 2000 // tempo de leitura p/ buscar informações (2s)
                urlConnection.connectTimeout = 2000 // tempo de coneção da aplicação com o servidor (2s)

                val statusCode = urlConnection.responseCode
                if (statusCode > 400) {
                    throw IOException("Erro na comunicação com o servidor!")
                }

                inputStream = urlConnection.inputStream
                val jsonAsString = inputStream.bufferedReader().use { it.readText() }
                val categories = toCategories(jsonAsString)

                handler.post {
                    // roda dentro da Ui Thread
                    callback.onResult(categories)
                }

            } catch (e: Exception) {
                val message = e.message ?: "Erro desconhecido"
                Log.e("Teste", message, e)

                handler.post {
                    callback.onFailure(message)
                }

            } finally {

                urlConnection?.disconnect()
            }
        }
    }

    private fun toCategories(jsonAsString: String): List<Category> {
        val categories = mutableListOf<Category>()

        val jsonRoot = JSONObject(jsonAsString)
        val jsonCategories = jsonRoot.getJSONArray("category")
        for (i in 0 until jsonCategories.length()) {
            val jsonCategory = jsonCategories.getJSONObject(i)

            val title = jsonCategory.getString("title")
            val jsonMovies = jsonCategory.getJSONArray("movie")

            val movies = mutableListOf<Movie>()
            for (j in 0 until jsonMovies.length()) {
                val jsonMovie = jsonMovies.getJSONObject(j)
                val id = jsonMovie.getInt("id")
                val coverUrl = jsonMovie.getString("cover_url")

                movies.add(Movie(id, coverUrl))
            }
            categories.add(Category(title, movies))
        }
        return categories
    }
}