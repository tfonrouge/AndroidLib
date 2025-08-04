package com.fonrouge.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.fonrouge.androidLib.commonServices.AppApi
import com.fonrouge.androidLib.helloResponse
import com.fonrouge.androidLib.viewModel.VMItem
import com.fonrouge.arelLib.model.Almacen
import com.fonrouge.fsLib.model.base.BaseDoc
import com.fonrouge.fsLib.types.StringId
import com.fonrouge.myapplication.ui.theme.MyApplicationTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

data class Person(
    override val _id: String,
    val name: String,
    val age: Int
) : BaseDoc<String>

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    AppApi.urlBase = "http://192.168.1.1:3000"
    val vm: VMItem<*, *, *, *>? = null
    val almacen = Almacen(
        _id = StringId("1"),
        clave = "UNO",
        nombre = "Almacen UNO"
    )
    println(almacen)
    val person = Person(
        _id = "1",
        name = name,
        age = 20
    )
    var clicked by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                CoroutineScope(scope.coroutineContext).launch {
                    attemptLogin()
                }
                clicked = !clicked
            }
        ) {
            if (clicked) {
                Text(text = helloResponse(person.name))
            }
            else
                Text(text = "Click me")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}

val httpClient = HttpClient(Android) {
    // Optional: Install Logging for debugging
    install(Logging) {
        level = LogLevel.ALL
    }

    // Configure the Android engine (optional)
    engine {
        connectTimeout = 10_000 // 10 seconds
        socketTimeout = 10_000 // 10 seconds
    }

    // Install ContentNegotiation for JSON serialization/deserialization
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        })
    }

    install(DefaultRequest) {
        contentType(ContentType.Application.Json)
    }

    // Optional: Default request settings
    defaultRequest {
        // Example: Add a default header
        // header("Authorization", "Bearer your_token")
    }
}

@Serializable
data class Post(val id: Int, val title: String, val body: String)

suspend fun attemptLogin() {
    withContext(Dispatchers.IO) {
        try {
            // GET request
            val response: HttpResponse =
                httpClient.get("https://jsonplaceholder.typicode.com/posts/1")
            val post = response.bodyAsText() // Or response.body<Post>() if using ContentNegotiation

            println("Fetched post: $post")

            // POST request
            val newPost =
                Post(id = 101, title = "My New Post", body = "This is the body of my new post.")
            val postResponse: HttpResponse? = try {
                httpClient.post("https://jsonplaceholder.typicode.com/posts") {
                    setBody(newPost)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
            println("Post response status: ${postResponse?.status?.value}")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
