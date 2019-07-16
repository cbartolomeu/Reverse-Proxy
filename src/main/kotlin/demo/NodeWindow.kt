package demo

import java.awt.Dimension
import java.awt.Font
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel

class NodeWindow(host: String) : JFrame() {
    var success = true

    init {
        title = host

        val switchBtn = JButton("ON").apply {
            minimumSize = Dimension(100, 100)
            font = Font("Arial", Font.PLAIN, 20)
        }

        switchBtn.addActionListener {
            success = !success
            switchBtn.text = if (success) "ON" else "OFF"
        }

        val panel = JPanel(GridLayout(1, 1))
        panel.add(switchBtn)

        contentPane.add(panel)

        pack()

        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(300, 150)
        setLocationRelativeTo(null)
        isResizable = false
    }
}
