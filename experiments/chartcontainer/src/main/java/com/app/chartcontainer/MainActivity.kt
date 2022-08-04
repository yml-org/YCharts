package com.app.chartcontainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.chartcontainer.ui.theme.YGraphsTheme
import com.ygraph.components.axis.Gravity
import com.ygraph.components.axis.YAxis

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YGraphsTheme {
                // A surface container using the 'background' color from the theme
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Yellow)
                ) {
                    // Left Aligned Yaxis
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        YAxis(
                            modifier = Modifier
                                .height(300.dp),
                            yMaxValue = 1000f,
                            yStepValue = 100f,
                            bottomPadding = 10.dp,
                            axisLabelFontSize = 14.sp, yLabelData = { index ->
                                return@YAxis index.toString() + "k"
                            }
                        )
                    }
                    // Right Aligned Yaxis
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        YAxis(
                            modifier = Modifier
                                .height(300.dp),
                            yMaxValue = 1000f,
                            yStepValue = 100f,
                            bottomPadding = 10.dp,
                            axisPos = Gravity.RIGHT,
                            axisLabelFontSize = 14.sp, yLabelData = { index ->
                                return@YAxis index.toString() + "k"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    YGraphsTheme {
        Greeting("Android")
    }
}
