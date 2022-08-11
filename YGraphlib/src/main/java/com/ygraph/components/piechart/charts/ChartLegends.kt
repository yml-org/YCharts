package com.ygraph.components.piechart.charts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Badge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ygraph.components.piechart.models.PieChartData


/**
 * @param pieChartData: Data list for the pie chart
 * @param padding: Padding of start
 * @param gridSize: Number of legends on each row
 **/

@Composable
fun Legends(
    pieChartData: PieChartData,
    legendTextColor: Color,
    padding: Dp = 15.dp,
    gridSize:Int = 4
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridSize),
        modifier = Modifier.padding(start = padding, top = padding),
        content = {
            items(pieChartData.slices.size)
            { index ->
                DisplayLegend(
                    color = pieChartData.slices[index].color,
                    legend = pieChartData.slices[index].label,
                    legendTextColor
                )
            }
        })

}


/**
 * @param color: Color of the legend
 * @param legend:Text of the badge
 * **/

@Composable
private fun DisplayLegend(
    color: Color,
    legend: String,
    legendTextColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Badge(
            modifier = Modifier.width(16.dp),
            containerColor = color
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = legend,
            color = legendTextColor
        )
    }
}
