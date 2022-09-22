package gameoflife

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.channels.Channel
import java.util.function.*

class Cell internal constructor(options: CellOptions) {
    private var alive: Boolean
    private val tickChannel: Channel<Boolean>
    private val resultChannel: Channel<Boolean>
    private val inChannels: List<Channel<Boolean>>
    private val outChannels: List<Channel<Boolean?>>

    init {
        alive = options.alive()
        tickChannel = options.tickChannel()
        resultChannel = options.resultChannel()
        inChannels = options.inChannels()
        outChannels = options.outChannels()
    }

    fun start() {
        GlobalScope.launch {
            run()
        }
    }

    private suspend fun run() {
        tickChannel.consumeEach {
            outChannels.forEach { ch: Channel<Boolean?> ->
                ch.send(
                    alive
                )
            } // announce liveness to neighbors

            var neighbors = 0
            for (inChannel in inChannels) {
                val e = inChannel.receive()
                if (e) ++neighbors
            }
            alive = nextState(alive, neighbors) // calculate next state based on game of life rules
            resultChannel.send(alive) // announce resulting next state
        }
    }

    private fun nextState(alive: Boolean, neighbors: Int): Boolean {
        return if (alive) {
            neighbors == 2 || neighbors == 3
        } else neighbors == 3
    }
}
