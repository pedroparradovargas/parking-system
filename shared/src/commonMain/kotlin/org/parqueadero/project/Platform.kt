package org.parqueadero.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform