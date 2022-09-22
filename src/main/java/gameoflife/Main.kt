package gameoflife

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.io.*

object Main {


    @JvmStatic
    fun main(args: Array<String>) {
        val a = Args.parse(args)
        val original = PatternParser.parseFile(a.patternFile())
        val rotated = if (a.rotate()) PatternParser.rotate(original) else original
        val pattern = PatternParser.pad(rotated, a.leftPadding(), a.topPadding(), a.rightPadding(), a.bottomPadding())
        val gridChannel = Channel<Array<BooleanArray>>(1024 * 1024) // channel carries aggregated liveness matrices
        val dimensions = Dimensions(pattern.size, pattern[0].size)
        val game = GameOfLife(dimensions, pattern, a.periodMilliseconds(), gridChannel)
        game.start()

        // See README.md
        val R = dimensions.rows()
        val C = dimensions.cols()
        val totalProcesses = R * C + 2
        val totalChannels = 2 * (C - 1) * R + 2 * (R - 1) * C + 4 * (R - 1) * (C - 1) + R * C * 2 + 1
        val consumer = if (a.enableBenchmark()) CountingOutput(totalProcesses, totalChannels) else ConsoleOutput(
            dimensions,
            totalProcesses,
            totalChannels
        )
        runBlocking {
            while (true) {
                consumer.accept(gridChannel.receive())
            }
        }
    }
}
