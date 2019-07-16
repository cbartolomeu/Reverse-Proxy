package util

inline fun <T> T?.orElse(block: () -> T): T = this ?: block()