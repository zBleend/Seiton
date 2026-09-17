package cl.seiton

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform