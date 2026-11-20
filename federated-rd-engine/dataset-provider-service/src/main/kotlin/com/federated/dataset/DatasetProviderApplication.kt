package com.federated.dataset

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DatasetProviderApplication

fun main(args: Array<String>) {
    runApplication<DatasetProviderApplication>(*args)
}
