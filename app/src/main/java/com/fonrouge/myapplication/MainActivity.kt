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
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                clicked = !clicked
            }
        ) {
            if (clicked)
                Text(text = helloResponse(person.name))
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