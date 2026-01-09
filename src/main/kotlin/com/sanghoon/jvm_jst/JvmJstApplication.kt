package com.sanghoon.jvm_jst

import com.uber.h3core.H3Core
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JvmJstApplication

fun main(args: Array<String>) {
    runApplication<JvmJstApplication>(*args)
}
