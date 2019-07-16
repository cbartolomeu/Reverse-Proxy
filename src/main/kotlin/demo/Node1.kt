package demo

import java.awt.EventQueue

fun main() {
    val win = NodeWindow("localhost:8081")
    EventQueue.invokeLater {
        win.isVisible = true
    }

    Node(8081, win).start()
}