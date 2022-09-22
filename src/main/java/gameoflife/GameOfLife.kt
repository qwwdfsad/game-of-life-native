package gameoflife

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import java.util.function.*
import java.util.stream.*

class GameOfLife internal constructor(
    private val dims: Dimensions,
    seed: Array<BooleanArray>,
    private val period: Int,
    private val gridChannel: Channel<Array<BooleanArray>>
) {
    private val cells: MutableList<Cell>
    private val tickChannels: List<List<Channel<Boolean>>>
    private val resultChannels: List<List<Channel<Boolean>>>

    init {
        cells = ArrayList()
        tickChannels = List(dims.rows()) { List(dims.cols()) { Channel<Boolean>(1024 * 1024) } }
        resultChannels = List(dims.rows()) { List(dims.cols()) { Channel<Boolean>(1024 * 1024) } }
        val grid = Array(dims.rows()) {
            arrayOfNulls<CellOptions>(
                dims.cols()
            )
        }
        dims.forEachRowCol { r: Int?, c: Int? ->
            grid[r!!][c!!] = CellOptions(
                r,
                c,
                seed[r][c],
                tickChannels[r][c],
                resultChannels[r][c],
                ArrayList(),
                ArrayList()
            )
        }
        dims.forEachRowCol { r: Int?, c: Int? ->
            val cell = grid[r!!][c!!]
            dims.forEachNeighbor(r, c) { ri: Int?, ci: Int? ->
                val other = grid[ri!!][ci!!]
                val ch = Channel<Boolean>(1024 * 1024)
                cell!!.inChannels().add(ch)
                other!!.outChannels().add(ch)
            }
        }
        dims.forEachRowCol { r: Int?, c: Int? -> cells.add(Cell(grid[r!!][c!!]!!)) }
    }

    fun start() {
        cells.forEach {
            it.start()
        }
        GlobalScope.launch {
            run()
        }
    }

    private suspend fun run() {
        while (true) {
            dims.forEachRowCol { r: Int, c: Int -> tickChannels[r][c].send(true) } // emit tick event for every cell
            val grid = Array(dims.rows()) { BooleanArray(dims.cols()) } // prepare result boolean matrix
            dims.forEachRowCol { r: Int?, c: Int? ->
                grid[r!!][c!!] = resultChannels[r][c].receive()
            } // populate matrix with results
            gridChannel.send(grid) // emit aggregated liveness matrix
            delay(period.toLong())
        }
    }


    private inline fun Dimensions.forEachRowCol(consumer: (Int, Int) -> Unit) {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                consumer(r, c)
            }
        }
    }
}
