package demo

import java.awt.EventQueue

fun main() {
    val win = NodeWindow("localhost:8082")
    EventQueue.invokeLater {
        win.isVisible = true
    }

    Node(8082, win).start()
}