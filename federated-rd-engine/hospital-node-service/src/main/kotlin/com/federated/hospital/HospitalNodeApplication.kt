package com.federated.hospital

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HospitalNodeApplication

fun main(args: Array<String>) {
    runApplication<HospitalNodeApplication>(*args)
}
