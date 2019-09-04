package demo

import appFeatures.simpleLogging.SimpleLogging
import appFeatures.throttling.Throttling
import clientFeatures.circuitBreaker.CircuitBreaker
import clientFeatures.loadBalancer.LoadBalancer
import clientFeatures.retry.Retry
import core.forward
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.atomicfu.atomic
import java.awt.EventQueue
import java.awt.Font
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFrame
import javax.swing.JPanel

class ReverseProxy(private val port: Int) {
    val client = atomic(HttpClient())
    val throttling = atomic(false)
    private val otherPipeline = ApplicationCallPipeline().apply { install(Throttling, Config.throttlingConfig) }

    fun start() {
        val menu = Menu()
        EventQueue.invokeLater {
            menu.isVisible = true
        }

        embeddedServer(Netty, port,
            module = {
                install(SimpleLogging)
                routing {
                    route("/") {
                        handle {
                            if (throttling.value) otherPipeline.execute(call, subject)
                            val hasResponse = call.response.status() != null // if the throttling pipeline responded
                            // Default is localhost:8081 in case not load balanced
                            if (!hasResponse) client.value.forward(call, "localhost:8081")
                        }
                    }
                }
            }
        ).start()
    }

    private inner class Menu : JFrame() {
        init {
            title = "RP Menu"

            val mainPanel = JPanel(GridLayout(2, 1))
            val optPanel = JPanel(GridLayout(2, 2))

            val btn = JButton("Update").apply {
                font = Font("Arial", Font.PLAIN, 40)
            }

            val lb = JCheckBox("Load Balancer")
            val retry = JCheckBox("Retry")
            val cb = JCheckBox("Circuit Breaker")
            val th = JCheckBox("Throttling")
            optPanel.add(lb)
            optPanel.add(retry)
            optPanel.add(cb)
            optPanel.add(th)

            btn.addActionListener {
                val newClient = HttpClient {
                    if (lb.isSelected)
                        install(LoadBalancer, Config.loadBalancerConfig)
                    if (retry.isSelected)
                        install(Retry, Config.retryConfig)
                    if (cb.isSelected)
                        install(CircuitBreaker, Config.circuitBreakerConfig)
                    throttling.getAndSet(th.isSelected)
                }

                client.getAndSet(newClient)
            }

            mainPanel.add(optPanel)
            mainPanel.add(btn)
            contentPane.add(mainPanel)
            pack()

            defaultCloseOperation = EXIT_ON_CLOSE
            setSize(500, 500)
            setLocationRelativeTo(null)
            isResizable = false
        }
    }
}

fun main() {
    ReverseProxy(8080).start()
}