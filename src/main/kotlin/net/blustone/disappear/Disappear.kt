package net.blustone.disappear

import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.PrivateChannel
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.utils.PermissionUtil
import java.util.prefs.Preferences
import javax.security.auth.login.LoginException
import kotlin.system.exitProcess

class Disappear {

    enum class Strategy {
        EAGER, LAZY
    }

    companion object {

        private var lastLine = 0

        private fun pl(string: String) {
            val str = string.padEnd(lastLine, ' ')
            print(str)
            lastLine = str.length
        }

        private fun pb() = repeat(lastLine) { print('\b') }

        private fun resp(text: String, choices: Map<Char, String> = mapOf('y' to "yes", 'n' to "no"), default: Char = 'y'): Char {
            print(text)
            print(" (")
            print(default.toUpperCase())
            choices.filter { it.key != default }.forEach { print(it.key) }
            print(") ")
            print(choices.asSequence().joinToString(", ") { "${it.key}=${it.value}" })
            print(' ')
            var response = readLine()
            while (response != null && response.length > 1) {
                print("Invalid response, try again: ")
                response = readLine()
            }
            return response?.firstOrNull() ?: default
        }

        private val prefs = Preferences.userNodeForPackage(Disappear::class.java)

        private var token: String? = null

        private fun TextChannel.deleteAllMessages(strategy: Strategy) {
            val perms = Permission.getPermissions(PermissionUtil.getEffectivePermission(this, guild.selfMember))
            if (!perms.contains(Permission.VIEW_CHANNEL) || !perms.contains(Permission.MESSAGE_READ)) {
                pl("No permissions to manage #$name in ${guild.name}, skipping...")
                println()
                return
            }
            pl("Retrieving messages from #$name in ${guild.name}...")
            val mh = history
            var ct = 0
            var delet = 0
            while (true) {
                val retrieved = mh.retrievePast(100).complete()
                if (retrieved == null || retrieved.isEmpty()) break
                if (strategy == Strategy.LAZY) for (m in retrieved.filter { it.author == jda.selfUser }) {
                    try {
                        m.delete().complete(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delet += 1
                    pb()
                    pl("Messages deleted from #$name in ${guild.name}: $delet")
                }
                ct += retrieved.size
                pb()
                pl("Retrieving messages from #$name in ${guild.name}: $ct messages")
            }
            val h = mh.retrievedHistory.filter { it.author == jda.selfUser }
            pb()
            pl("Messages successfully retrieved from #$name in ${guild.name}: ${h.size}")

            if (strategy == Strategy.EAGER) {
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
            }
            pb()
            pl("Deleted all ${h.size} messages in #$name in ${guild.name}!")
            println()
        }

        private fun PrivateChannel.deleteAllMessages(strategy: Strategy) {
            val dn = "@${user.name}#${user.discriminator}"
            pl("Retrieving messages from $dn...")
            val mh = history
            var ct = 0
            var delet = 0
            while (true) {
                val retrieved = mh.retrievePast(100).complete()
                if (retrieved == null || retrieved.isEmpty()) break
                ct += retrieved.size
                if (strategy == Strategy.LAZY) for (m in retrieved.filter { it.author == jda.selfUser }) {
                    try {
                        m.delete().complete(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delet += 1
                    pb()
                    pl("Messages deleted from #$name in $dn: $delet")
                }
                pb()
                pl("Retrieving messages from $dn: $ct messages")
            }
            val h = mh.retrievedHistory.filter { it.author == jda.selfUser }
            pb()
            pl("Messages successfully retrieved from $dn: ${h.size}")
            if (strategy == Strategy.EAGER) {
                for (l in h) {
                    try {
                        l.delete().complete(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delet += 1
                    pb()
                    pl("Messages deleted from $dn: $delet/${h.size}")
                }
            }
            pb()
            pl("Deleted all ${h.size} messages in $dn!")
            println()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            // Request token
            token = prefs.get("token", null)
            if (token == null) {
                print("No token found in preferences, please enter one: ")
                var t = readLine()
                while (t.isNullOrBlank()) {
                    t = readLine()
                }
                token = t
            } else {
                when (resp("Token found in preferences, would you like to use it?")) {
                    'n' -> {
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
                                prefs.put("token", token)
                                prefs.flush()
                                println("Ready!")
                                println("Logged in as ${it.jda.selfUser.name}!")
                                println("Starting...")
                                val jda = it.jda
                                val guilds = jda.guilds
                                val strategy = when (resp("Select a retrieval strategy", mapOf('l' to "lazy", 'e' to "eager"), 'e')) {
                                    'l' -> Strategy.LAZY
                                    'e' -> Strategy.EAGER
                                    else -> throw RuntimeException()
                                }
                                when (resp("Delete from all guilds or filter manually?", mapOf('a' to "all", 'f' to "filter"), 'a')) {
                                    'a' -> {
                                        guilds.forEach {
                                            it.textChannels.forEach {
                                                it.deleteAllMessages(strategy)
                                            }
                                        }
                                    }
                                    'f' -> {
                                        guilds.forEach {
                                            if (resp("Purge ${it.name}?") == 'y') {
                                                it.textChannels.forEach {
                                                    it.deleteAllMessages(strategy)
                                                }
                                            }
                                        }
                                    }
                                }
                                when (resp("Delete from all DMs or filter manually?", mapOf('a' to "all", 'f' to "filter"), 'a')) {
                                    'a' -> {
                                        jda.privateChannels.forEach {
                                            it.deleteAllMessages(strategy)
                                        }
                                    }
                                    'f' -> {
                                        jda.privateChannels.forEach {
                                            if (resp("Purge ${it.name}?") == 'y') {
                                                it.deleteAllMessages(strategy)
                                            }
                                        }
                                    }
                                }
                                println("Done")
                                exitProcess(0)
                            }
                        }
                    })
            while (true) {
                try {
                    println("Building JDA instance...")
                    jdaBuilder.buildBlocking()
                    break
                } catch (e: LoginException) {
                    println("Error with credentials: ${e.message}")
                    print("Please enter a token: ")
                    var t = readLine()
                    while (t.isNullOrBlank()) {
                        t = readLine()
                    }
                    token = t
                    jdaBuilder.setToken(t)
                    continue
                }
            }
        }

    }

}
