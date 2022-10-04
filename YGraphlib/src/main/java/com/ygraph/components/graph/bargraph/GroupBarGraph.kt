package com.ygraph.components.graph.bargraph


import android.annotation.SuppressLint
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ygraph.components.axis.XAxis
import com.ygraph.components.axis.YAxis
import com.ygraph.components.common.extensions.RowClip
import com.ygraph.components.common.extensions.collectIsTalkbackEnabledAsState
import com.ygraph.components.common.extensions.getMaxElementInYAxis
import com.ygraph.components.common.extensions.isTapped
import com.ygraph.components.common.model.Point
import com.ygraph.components.common.utils.GraphConstants.DEFAULT_YAXIS_BOTTOM_PADDING
import com.ygraph.components.graph.bargraph.models.BarData
import com.ygraph.components.graph.bargraph.models.GroupBarGraphData
import com.ygraph.components.graph.bargraph.models.SelectionHighlightData
import com.ygraph.components.graphcontainer.container.ScrollableCanvasContainer
import com.ygraph.components.graphcontainer.container.checkAndGetMaxScrollOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 *
 * GroupBarGraph compose method for drawing group bar graph.
 * @param modifier: All modifier related properties
 * @param groupBarGraphData : All data needed to group bar graph
 * @see com.ygraph.components.graph.bargraph.models.GroupBarGraphData Data class to save all
 * params related to Bar Graph
 */
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun GroupBarGraph(modifier: Modifier, groupBarGraphData: GroupBarGraphData) {
    Column(modifier) {
        with(groupBarGraphData.barPlotData) {
            var visibility by remember { mutableStateOf(false) }
            var identifiedPoint by remember { mutableStateOf(BarData(Point(0f, 0f))) }
            var xOffset by remember { mutableStateOf(0f) }
            var tapOffset by remember { mutableStateOf(Offset(0f, 0f)) }
            var isTapped by remember { mutableStateOf(false) }
            var columnWidth by remember { mutableStateOf(0f) }
            var horizontalGap by remember { mutableStateOf(0f) }
            var rowHeight by remember { mutableStateOf(0f) }
            val paddingRight = groupBarGraphData.paddingEnd
            val valueList = groupBarList.map { it.yMax }
            val bgColor = MaterialTheme.colors.surface
            val localDensity = LocalDensity.current
            val talkbackEnabled by LocalContext.current.collectIsTalkbackEnabledAsState()

            //-------------For scrolling-------------------
            val composableScope = rememberCoroutineScope()
            val scrollOffset = remember { mutableStateOf(0f) }
            val maxScrollOffset = remember { mutableStateOf(0f) }
            val scrollState = rememberScrollableState { delta ->
                scrollOffset.value -= delta
                scrollOffset.value = checkAndGetMaxScrollOffset(
                    scrollOffset.value,
                    maxScrollOffset.value
                )
                delta
            }

            var isDefaultTapped by remember { mutableStateOf(false) }
            var selectedIndex by remember { mutableStateOf(0) }
            var selectedSubIndex by remember { mutableStateOf(0) }
            var yBottom by remember { mutableStateOf(0f) }
            var yOffset by remember { mutableStateOf(0f) }
            var xLeft by remember { mutableStateOf(0f) }
            var insideOffset by remember { mutableStateOf(0f) }
            var dragLocks = mutableMapOf<Int, Pair<BarData, Offset>>()

            //-------------For accessibility-------------------
            val focusRequesterContainer = remember { FocusRequester() }
            val coroutineScope = rememberCoroutineScope()
            var isContainerFocused by remember { mutableStateOf(false) }
            var containerFocusedText by remember { mutableStateOf("") }
            val focusRequesterNextBtn = remember { FocusRequester() }
            val focusRequesterPrevBtn = remember { FocusRequester() }

            val xMax = groupBarList.size
            val yMax = valueList.maxOrNull() ?: 0f
            val xAxisData =
                groupBarGraphData.xAxisData.copy(
                    axisStepSize = ((barStyle.barWidth * groupingSize) + barStyle.paddingBetweenBars),
                    shouldDrawAxisLineTillEnd = true,
                    steps = groupBarList.size - 1,
                    startDrawPadding = LocalDensity.current.run { columnWidth.toDp() }
                )
            val yAxisData = groupBarGraphData.yAxisData.copy(
                axisBottomPadding = LocalDensity.current.run { rowHeight.toDp() }
            )

            val maxElementInYAxis = getMaxElementInYAxis(yMax, yAxisData.steps)

            if (!groupBarGraphData.showXAxis) {
                rowHeight = LocalDensity.current.run { DEFAULT_YAXIS_BOTTOM_PADDING.dp.toPx() }
            }

            val scaffoldState: ScaffoldState = rememberScaffoldState()

            Scaffold(scaffoldState = scaffoldState) {
                Box {
                    Column {
                        ScrollableCanvasContainer(
                            modifier = modifier
                                .focusRequester(focusRequesterContainer)
                                .focusable()
                                .semantics {
                                    contentDescription =
                                        if (isContainerFocused) containerFocusedText else "Default content"
                                },
                            scrollOffset = scrollOffset.value,
                            maxScrollOffset = { maxScroll ->
                                maxScrollOffset.value = maxScroll
                            },
                            scrollState = scrollState,
                            containerBackgroundColor = groupBarGraphData.backgroundColor,
                            calculateMaxDistance = { xZoom ->
                                horizontalGap = groupBarGraphData.horizontalExtraSpace.toPx()
                                val xLeft = columnWidth + horizontalGap
                                xOffset =
                                    ((barStyle.barWidth.toPx() * groupingSize) + barStyle.paddingBetweenBars.toPx()) * xZoom
                                getMaxScrollDistance(
                                    columnWidth,
                                    xMax.toFloat(),
                                    0f,
                                    xOffset,
                                    xLeft,
                                    paddingRight.toPx(),
                                    size.width
                                )
                            },
                            onDraw = { scrollOffset, xZoom ->
                                 yBottom = size.height - rowHeight
                                 yOffset =
                                    ((yBottom - yAxisData.axisTopPadding.toPx()) / maxElementInYAxis)
                                xOffset =
                                    ((barStyle.barWidth.toPx() * groupingSize) + barStyle.paddingBetweenBars.toPx()) * xZoom
                                 xLeft =
                                    columnWidth + horizontalGap
                               // val dragLocks = mutableMapOf<Int, Pair<BarData, Offset>>()

                                // Draw bar lines
                                groupBarList.forEachIndexed { index, groupBarData ->
                                    var insideOffset = 0f

                                    groupBarData.barList.forEachIndexed { subIndex, individualBar ->
                                        val drawOffset = getGroupBarDrawOffset(
                                            index, individualBar.point.y, xOffset, xLeft,
                                            scrollOffset, yBottom, yOffset, 0f
                                        )
                                        val height = yBottom - drawOffset.y

                                        val individualOffset =
                                            Offset(drawOffset.x + insideOffset, drawOffset.y)

                                        // drawing each individual bars
                                        drawGroupBarGraph(
                                            groupBarGraphData,
                                            individualOffset,
                                            height,
                                            subIndex
                                        )
                                        insideOffset += barStyle.barWidth.toPx()

                                        val middleOffset =
                                            Offset(
                                                individualOffset.x + barStyle.barWidth.toPx() / 2,
                                                drawOffset.y
                                            )
                                        // store the tap points for selection
                                        if (isTapped && middleOffset.isTapped(
                                                tapOffset,
                                                barStyle.barWidth.toPx(),
                                                yBottom,
                                                groupBarGraphData.tapPadding.toPx()
                                            )
                                        ) {
                                            dragLocks[0] = individualBar to individualOffset
                                        }
                                    }

                                    if (groupBarGraphData.groupSeparatorConfig.showSeparator && index != groupBarList.size - 1) {
                                        // drawing each Group Separator bars
                                        val yOffset2 = (yBottom - yAxisData.axisTopPadding.toPx())
                                        val height = yBottom - yAxisData.axisTopPadding.toPx()
                                        val drawOffset2 = getGroupBarDrawOffset(
                                            index,
                                            rowHeight,
                                            xOffset,
                                            xLeft,
                                            scrollOffset,
                                            yBottom,
                                            yOffset2,
                                            0f
                                        )
                                        val xOffset2 = (drawOffset2.x
                                                + insideOffset + (barStyle.paddingBetweenBars.toPx() / 2) -
                                                groupBarGraphData.groupSeparatorConfig.separatorWidth.toPx() / 2)
                                        val individualOffset =
                                            Offset(xOffset2, yAxisData.axisTopPadding.toPx())

                                        drawGroupSeparator(
                                            individualOffset,
                                            height,
                                            groupBarGraphData.groupSeparatorConfig.separatorWidth.toPx(),
                                            groupBarGraphData.groupSeparatorConfig.separatorColor,
                                            groupBarGraphData
                                        )
                                    }
                                }
                                drawUnderScrollMask(columnWidth, paddingRight, bgColor)

                                if (barStyle.selectionHighlightData != null) {
                                    // highlighting the selected bar and showing the data points
                                    identifiedPoint = highlightGroupBar(
                                        dragLocks,
                                        visibility,
                                        identifiedPoint,
                                        barStyle.selectionHighlightData,
                                        isTapped,
                                        columnWidth,
                                        yBottom,
                                        paddingRight,
                                        yOffset,
                                        barStyle.barWidth
                                    )
                                }
                            },
                            drawXAndYAxis = { scrollOffset, xZoom ->
                                val points = mutableListOf<Point>()
                                for (index in groupBarList.indices) {
                                    points.add(Point(index.toFloat(), 0f))
                                }

                                if (groupBarGraphData.showXAxis) {
                                    XAxis(
                                        xAxisData = xAxisData,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                            .clip(
                                                RowClip(
                                                    columnWidth,
                                                    paddingRight
                                                )
                                            )
                                            .onGloballyPositioned {
                                                rowHeight = it.size.height.toFloat()
                                            },
                                        xStart = columnWidth + horizontalGap + LocalDensity.current.run {
                                            (barStyle.barWidth.toPx() * groupingSize) / 2
                                        },
                                        scrollOffset = scrollOffset,
                                        zoomScale = xZoom,
                                        graphData = points,
                                    )
                                }

                                if (groupBarGraphData.showYAxis) {
                                    YAxis(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .fillMaxHeight()
                                            .wrapContentWidth()
                                            .onGloballyPositioned {
                                                columnWidth = it.size.width.toFloat()
                                            },
                                        yAxisData = yAxisData,
                                    )
                                }
                            },
                            onPointClicked = { offset: Offset, _: Float ->
                                isTapped = true
                                visibility = true
                                tapOffset = offset
                            },
                            onScroll = {
                                /*  isTapped = false
                                  visibility = false*/
                            }
                        )
                    }
                    if (talkbackEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = groupBarGraphData.accessibilityConfig.paddingHorizontal,
                                    vertical = groupBarGraphData.accessibilityConfig.paddingVertical
                                )
                                .align(groupBarGraphData.accessibilityConfig.alignment),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(modifier = Modifier
                                .focusRequester(focusRequesterNextBtn)
                                .focusable(), onClick = {
                                isTapped = true
                                visibility = true
                                selectedSubIndex += 1
                                val xStep: Float =
                                    if (selectedSubIndex > groupBarList[selectedIndex].barList.size - 1) {
                                        selectedSubIndex = 0
                                        selectedIndex += 1
                                        insideOffset = 0f
                                        ((localDensity.run { barStyle.barWidth.toPx() }) + localDensity.run { barStyle.paddingBetweenBars.toPx() })
                                    } else {
                                        insideOffset += localDensity.run { barStyle.barWidth.toPx() }
                                        if (selectedSubIndex == 1 && selectedIndex == 0) {
                                            localDensity.run { barStyle.barWidth.toPx() } + localDensity.run { groupBarGraphData.horizontalExtraSpace.toPx() }
                                        } else {
                                            localDensity.run { barStyle.barWidth.toPx() }
                                        }
                                    }
                                composableScope.launch {
                                    scrollState.scrollBy(-xStep)
                                }

                                val drawOffset = getGroupBarDrawOffset(
                                    selectedIndex,
                                    groupBarList[selectedIndex].barList[selectedSubIndex].point.y,
                                    xOffset,
                                    xLeft,
                                    scrollOffset.value + xStep,
                                    yBottom,
                                    yOffset,
                                    0f
                                )
                                val individualOffset =
                                    Offset(drawOffset.x + insideOffset, drawOffset.y)

                                tapOffset = individualOffset
                                val middleOffset =
                                    Offset(individualOffset.x + localDensity.run { barStyle.barWidth.toPx() } / 2,
                                        drawOffset.y)
                                // store the tap points for selection
                                if (isTapped && middleOffset.isTapped(
                                        tapOffset,
                                        localDensity.run { barStyle.barWidth.toPx() },
                                        yBottom,
                                        localDensity.run { groupBarGraphData.tapPadding.toPx() }
                                    )
                                ) {
                                    dragLocks[0] =
                                        groupBarList[selectedIndex].barList[selectedSubIndex] to individualOffset
                                }
                                val dragLockValue = dragLocks.values.firstOrNull()?.first?.point
                                val valueY = dragLockValue?.y
                                val xLabel = "In Group bar $selectedIndex , bar at : ${dragLockValue?.x?.toInt()} is selected "
                                val yLabel = " with value : ${String.format("%.2f", valueY)}"
                                coroutineScope.launch {
                                    isContainerFocused = true
                                    containerFocusedText = "$xLabel $yLabel"
                                    focusRequesterContainer.requestFocus()
                                    delay(6000)
                                    isContainerFocused = false
                                    focusRequesterNextBtn.requestFocus()
                                }
                            }) {
                                Text(text = groupBarGraphData.accessibilityConfig.nextButtonText)
                            }

                            Button(modifier = Modifier
                                .focusRequester(focusRequesterPrevBtn)
                                .focusable(), onClick = {
                                val xStep: Float
                                isTapped = true
                                visibility = true
                                selectedSubIndex -= 1
                                if (selectedSubIndex < 0) {
                                    selectedIndex -= 1
                                    if (selectedIndex >= 0) {
                                        selectedSubIndex =
                                            groupBarList[selectedIndex].barList.size - 1
                                        insideOffset =
                                            ((localDensity.run { barStyle.barWidth.toPx() }) * (groupBarList[selectedIndex].barList.size - 1))
                                    } else {
                                        selectedIndex = 0
                                        selectedSubIndex = 0
                                        insideOffset = 0f
                                    }
                                    xStep =
                                        ((localDensity.run { barStyle.barWidth.toPx() }) + localDensity.run { barStyle.paddingBetweenBars.toPx() })

                                } else {
                                    insideOffset -= localDensity.run { barStyle.barWidth.toPx() }
                                    xStep = localDensity.run { barStyle.barWidth.toPx() }
                                }
                                composableScope.launch {
                                    scrollState.scrollBy(+xStep)
                                }

                                val drawOffset = getGroupBarDrawOffset(
                                    selectedIndex,
                                    groupBarList[selectedIndex].barList[selectedSubIndex].point.y,
                                    xOffset,
                                    xLeft,
                                    scrollOffset.value - xStep,
                                    yBottom,
                                    yOffset,
                                    0f
                                )
                                val individualOffset =
                                    Offset(drawOffset.x + insideOffset, drawOffset.y)

                                tapOffset = individualOffset
                                val middleOffset =
                                    Offset(individualOffset.x + localDensity.run { barStyle.barWidth.toPx() } / 2,
                                        drawOffset.y)
                                // store the tap points for selection
                                if (isTapped && middleOffset.isTapped(
                                        tapOffset,
                                        localDensity.run { barStyle.barWidth.toPx() },
                                        yBottom,
                                        localDensity.run { groupBarGraphData.tapPadding.toPx() }
                                    )
                                ) {
                                    dragLocks[0] =
                                        groupBarList[selectedIndex].barList[selectedSubIndex] to individualOffset
                                }
                                val dragLockValue = dragLocks.values.firstOrNull()?.first?.point
                                val valueY = dragLockValue?.y
                                val xLabel = "In Group bar $selectedIndex , bar at : ${dragLockValue?.x?.toInt()} is selected "
                                val yLabel = " with value : ${String.format("%.2f", valueY)}"
                                coroutineScope.launch {
                                    isContainerFocused = true
                                    containerFocusedText = "$xLabel $yLabel"
                                    focusRequesterContainer.requestFocus()
                                    delay(6000)
                                    isContainerFocused = false
                                    focusRequesterPrevBtn.requestFocus()
                                }

                            }) {
                                Text(text = groupBarGraphData.accessibilityConfig.prevButtonText)
                            }
                        }
                    }
                }
                LaunchedEffect(key1 = Unit, block = {
                    // T0D0: To be removed once we have support to get callback on announcement completely
                    delay(1000)
                    focusRequesterNextBtn.requestFocus()
                })
            }
        }
    }
}

/**
 *
 * Used to draw the highlighted text
 * @param identifiedPoint : Selected points
 * @param selectedOffset: Offset selected
 * @param barWidth: Width of single bar
 * @param highlightData: Data for the highlight section
 */
private fun DrawScope.drawGroupHighlightText(
    identifiedPoint: BarData,
    selectedOffset: Offset,
    barWidth: Dp,
    highlightData: SelectionHighlightData
) {
    val centerPointOfBar = selectedOffset.x + barWidth.toPx() / 2
    // Drawing the highlighted background and text
    highlightData.drawGroupBarPopUp(this, selectedOffset, identifiedPoint, centerPointOfBar)
}


/**
 *
 * Used to draw the individual bars
 * @param barGraphData : all meta data related to the bar graph
 * @param drawOffset: topLeft offset for the drawing the bar
 * @param height : height of the bar graph
 * @param subIndex : Index of the bar
 */
private fun DrawScope.drawGroupBarGraph(
    barGraphData: GroupBarGraphData, drawOffset: Offset,
    height: Float,
    subIndex: Int
) {
    with(barGraphData.barPlotData) {
        val color = barColorPaletteList[subIndex]
        drawRoundRect(
            color = color,
            topLeft = drawOffset,
            size = Size(barGraphData.barPlotData.barStyle.barWidth.toPx(), height),
            cornerRadius = CornerRadius(
                barStyle.cornerRadius.toPx(),
                barStyle.cornerRadius.toPx()
            ),
            style = barStyle.barDrawStyle,
            blendMode = barStyle.barBlendMode
        )
    }
}


/**
 *
 * returns identified point and displaying the data points and highlighted bar .
 * @param dragLocks : Mutable map of BarData and drawOffset.
 * @param visibility : Flag to control the visibility of highlighted text.
 * @param identifiedPoint: selected bar data.
 * @param selectionHighlightData: Data related to the bar graph highlight.
 * @param isDragging : Boolean flag for the dragging status.
 * @param columnWidth : Width of the Y axis.
 * @param yBottom : Bottom padding.
 * @param paddingRight : Right padding.
 * @param yOffset : Distance between two y points.
 * @param barWidth : Width of each bar.
 */
fun DrawScope.highlightGroupBar(
    dragLocks: MutableMap<Int, Pair<BarData, Offset>>,
    visibility: Boolean,
    identifiedPoint: BarData,
    selectionHighlightData: SelectionHighlightData?,
    isDragging: Boolean,
    columnWidth: Float,
    yBottom: Float,
    paddingRight: Dp,
    yOffset: Float,
    barWidth: Dp,
): BarData {
    var mutableIdentifiedPoint: BarData = identifiedPoint
    // Handle the show the selected bar
    if (isDragging) {
        val x = dragLocks.values.firstOrNull()?.second?.x
        if (x != null) {
            mutableIdentifiedPoint = dragLocks.values.map { it.first }.first()
        }

        // Draw highlight bar on selection
        if (selectionHighlightData?.isHighlightBarRequired == true) {
            dragLocks.values.firstOrNull()?.let { (barData, location) ->
                val (xPoint, yPoint) = location
                if (xPoint >= columnWidth && xPoint <= size.width - paddingRight.toPx()) {
                    val y1 = yBottom - ((barData.point.y - 0) * yOffset)
                    selectionHighlightData.drawHighlightBar(
                        this,
                        xPoint,
                        yPoint,
                        barWidth.toPx(),
                        yBottom - y1
                    )
                }
            }
        }
    }

    val selectedOffset = dragLocks.values.firstOrNull()?.second
    if (visibility && selectedOffset != null && selectionHighlightData != null) {
        drawGroupHighlightText(
            mutableIdentifiedPoint,
            selectedOffset,
            barWidth,
            selectionHighlightData
        )
    }
    return mutableIdentifiedPoint
}

/**
 * returns the draw offset for bar graph.
 * @param yMin: Minimum value on the y axis
 * @param xOffset: Distance between bars
 * @param yOffset: Distance between y axis points
 * @param xLeft: X starting point of bar graph
 * @param scrollOffset: Scroll offset
 * @param yBottom: Y starting point of bar graph
 */
fun getGroupBarDrawOffset(
    x: Int, y: Float, xOffset: Float,
    xLeft: Float, scrollOffset: Float, yBottom: Float, yOffset: Float, yMin: Float
): Offset {
    val x1 = (x * xOffset) + xLeft - scrollOffset
    val y1 = yBottom - ((y - yMin) * yOffset)
    return Offset(x1, y1)
}

/**
 *
 * Used to draw the group separator
 * @param barGraphData: [GroupBarGraphData]
 * @param drawOffset: topLeft offset for the drawing the separator
 * @param height : height of the separator
 * @param width : width of the separator
 * @param color : color of the separator
 */
private fun DrawScope.drawGroupSeparator(
    drawOffset: Offset,
    height: Float,
    width: Float,
    color: Color,
    barGraphData: GroupBarGraphData,
) {
    drawRoundRect(
        color = color,
        topLeft = drawOffset,
        size = Size(width, height),
        blendMode = barGraphData.groupSeparatorConfig.separatorBlendMode
    )
}

