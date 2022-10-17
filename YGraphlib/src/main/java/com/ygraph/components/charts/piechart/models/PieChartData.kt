package com.ygraph.components.charts.piechart.models

import androidx.compose.ui.graphics.Color
import com.ygraph.components.common.model.PlotData
import com.ygraph.components.common.model.PlotType
import com.ygraph.components.charts.piechart.utils.sum

data class PieChartData(val slices: List<Slice>, override val plotType: PlotType) : PlotData {
    val totalLength: Float
        get() {
            return slices.sum()
        }

    data class Slice(
        val label: String,
        val value: Float,
        val color: Color,
        val sliceDescription: (Int) -> String = { slicePercentage ->
            "Slice name : $label  \nPercentage  : $slicePercentage %"
        }
    )
}