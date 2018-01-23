package net.blustone.disappear

import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.utils.PermissionUtil
import java.util.prefs.Preferences
import javax.security.auth.login.LoginException

class Disappear {

    companion object {

        private var lastLine = 0

        private fun pl(string: String) {
            val str = string.padEnd(lastLine, ' ')
            print(str)
            lastLine = str.length
        }

        private fun pb() = repeat(lastLine) { print('\b') }

        private fun resp(default: String = "y"): String {
            var r: String
            do r = readLine() ?: default while (r != "y" || r != "n")
            return r
        }

        private val prefs = Preferences.userNodeForPackage(Disappear::class.java)

        private var token: String? = null

        private fun TextChannel.deleteAllMessages() {
            val mh = history
            while (true) {
                val retrieved = mh.retrievePast(100).complete()
                if (retrieved == null || retrieved.isEmpty()) break
            }
            val h = mh.retrievedHistory.filter { it.author == jda.selfUser }
            pl("Messages successfully retrieved from #$name in ${guild.name}: ${h.size}")
            var delet = 0
            for (l in h) {
                try {
                    l.delete().complete(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delet += 1
                pb()
                pl("Messages deleted from #$name in ${guild.name}: $delet/${h.size}")
            }
            pb()
            pl("Deleted all ${h.size} messages in #$name in ${guild.name}!")
            println()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            // Request token
            token = prefs.get("token", null)
            if (token == null) {
                println("No token found in preferences, please enter one: ")
                var t = readLine()
                while (t.isNullOrBlank()) {
                    t = readLine()
                }
                token = t
            } else {
                println("Token found in preferences, would you like to use it? (Y/n) ")
                val r = resp("y")
                when (r) {
                    "n" -> {
                        println("Please enter a token: ")
                        var t = readLine()
                        while (t.isNullOrBlank()) {
                            t = readLine()
                        }
                        token = t
                    }
                }
            }
            val jdaBuilder = JDABuilder(AccountType.CLIENT)
                    .setToken(token)
                    .addEventListener(EventListener {
                        when (it) {
                            is ReadyEvent -> {
                                println("Ready!")
                                println("Logged in as ${it.jda.selfUser.name}!")
                                println("Starting...")
                                val jda = it.jda
                                val guilds = jda.guilds
                                guilds.forEach {
                                    it.textChannels.forEach {
                                        it.deleteAllMessages()
                                    }
                                }
                            }
                        }
                    })
            while (true) {
                try {
                    jdaBuilder.buildBlocking()
                    break
                } catch (e: LoginException) {
                    println("Error with credentials: ${e.message}")
                    println("Please enter a token: ")
                    var t = readLine()
                    while (t.isNullOrBlank()) {
                        t = readLine()
                    }
                    jdaBuilder.setToken(t)
                    continue
                }
            }
        }

    }

}
