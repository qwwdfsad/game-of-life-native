## Game of Life Native

[![Build with GraalVM Native Image](https://github.com/graalvm/game-of-life-native/actions/workflows/main.yaml/badge.svg)](https://github.com/graalvm/game-of-life-native/actions/workflows/main.yaml)

Game of Life Native is a Java implementation by @ebarlas on GitHub, of [Conway's Game of Life](https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life)
using [communicating sequential processes (CSP)](https://en.wikipedia.org/wiki/Communicating_sequential_processes). 

Each grid cell is an independent process and all cell communication occurs via channels.

It's built atop virtual threads, defined in [JDK Enhancement Proposal (JEP) 425](https://openjdk.java.net/jeps/425).

The virtual threads feature is part of [Project Loom](https://openjdk.java.net/projects/loom/).

Prior to Project Loom and virtual threads, CSP style programming in this manner simply wasn't available in Java.

![Channels](images/game-of-life-channels.png)

# Build

> JDK19-based GraalVM Native Image 22.3 or later is required (set `JAVA_HOME` to its installation directory).

Build native executable with GraalVM Native Image:
```
mvn -Pnative package
```

Run:
```
./target/game-of-life
```

## Command Line Arguments

Command line arguments are optional.

```
./target/game-of-life patterns/spaceship.txt 20 50 50 5 5 false true
```

1. Pattern text file, ex. `patterns/spaceship.txt`
2. Game of Life simulation period milliseconds, ex. `25`
3. Left padding columns, ex. `50`
4. Top padding rows, ex. `50`
5. Right padding columns, ex. `5`
6. Bottom padding rows, ex. `5`
7. Rotate boolean flag, ex. `false`
8. Benchmark mode boolean flag, ex. `true`

## Patterns

The [patterns](patterns) directory contains text-encoded patters taken from 
Life Lexicon located at: https://people.sc.fsu.edu/~jburkardt/m_src/exm/lexicon.txt

The lexicon is copyright (C) Stephen Silver, 1997-2005.

The full list of contributors can be found under the credits section of the website.

## Processes

Every cell runs in its own process, defined in [Cell.java](src/main/java/gameoflife/Cell.java). 
Cell processes communicate with each other via channels.

The simulation runs in its own process, defined in [GameOfLife.java](src/main/java/gameoflife/GameOfLife.java).

Finally, the viewer runs in its own process, defined in [Main.java](src/main/java/gameoflife/Main.java).

* Cell processes: `R * C`
* Simulation processes: `1`
* Viewer processes: `1`
* Total processes: `R * C + 2`

## Channels

A pair of channels, one in each direction, exists for every pair of neighbor cells.

* Vertical segments: `(C - 1) * R` 
* Horizontal segments: `(R - 1) * C` 
* Interior vertices: `(R - 1) * (C - 1)`
* Total cell-to-cell channels: `[2 * (C - 1) * R] + [2 * (R - 1) * C] + [4 * (R - 1) * (C - 1)]`

Additionally, each cell has a channel for receiving a tick event and
and a channel for emitting results after each simulation tick.

* Tick channels: `R * C`
* Result channels: `R * C`

Finally, a channel is used to communicate a full liveness matrix to
the main application consumer.

* Total channels: `2 * (C - 1) * R + 2 * (R - 1) * C + 4 * (R - 1) * (C - 1) + R * C * 2 + 1`

## Benchmark

The following command results in a grid of 50,000 cells (250 x 200):

That results in `50,002` virtual threads and `497,305` channels.

```
./target/game-of-life patterns/gosper_glider_gun.txt 0 2 2 212 189
```

It's a demonstration of the viability of virtual threads in a highly concurrent, computationally intensive application.


## Comparison

Target CPU: 2,3 GHz 8-Core Intel Core i9, 32 GM RAM.

Few notes to avoid result misinterpretation:

* ~31-33 ticks/s is a maximum achievable score **on my machine** on this benchmark. Probably throttled by the console throughput, requires further investigation
* The overall comparison is quite noisy 
* Starting from ~20k virtual threads, Loom results become really noisy and may fluctuate by ~50%
* This is not a real benchmark (in terms of precision, reproducibility and meticulous analysis) and should be treated as a toy example
* This benchmark may compare incomparable: e.g. coroutines provide cancellation of of the box and use a tailored scheduler, while virtual threads leverage ForkJoinPool and use a different channel data structure

| Target/Benchmark        | gosper_glider_gun.txt 0 2 2 212 189 | patterns/gosper_glider_gun.txt 0 2 2 106 189 | patterns/spaceship.txt 20 50 50 5 5 |
|-------------------------|-------------------------------------|----------------------------------------------|-----------------------------------------------
| Loom native image       | 8 ticks/s                           | 12 ticks/s                                   |                31 ticks/s
| Coroutines native image | 33 ticks/s                          | 31 ticks/s                                   |                31 ticks/s 

