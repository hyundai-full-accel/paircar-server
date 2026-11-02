package com.fullaccel.paircarserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PaircarServerApplication

fun main(args: Array<String>) {
    runApplication<PaircarServerApplication>(*args)
}
