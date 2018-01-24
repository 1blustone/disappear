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
import kotlin.system.exitProcess

class Disappear {

    companion object {

        private var lastLine = 0

        private fun pl(string: String) {
            val str = string.padEnd(lastLine, ' ')
            print(str)
            lastLine = str.length
        }

        private fun pb() = repeat(lastLine) { print('\b') }

        private fun resp(default: String = "y") = readLine() ?: default

        private val prefs = Preferences.userNodeForPackage(Disappear::class.java)

        private var token: String? = null

        private fun TextChannel.deleteAllMessages() {
            val perms = Permission.getPermissions(PermissionUtil.getEffectivePermission(this, guild.selfMember))
            if (!perms.contains(Permission.VIEW_CHANNEL) || !perms.contains(Permission.MESSAGE_READ)) {
                pl("No permissions to manage #$name in ${guild.name}, skipping...")
                println()
                return
            }
            pl("Retrieving messages from #$name in ${guild.name}...")
            val mh = history
            var ct = 0
            while (true) {
                val retrieved = mh.retrievePast(100).complete()
                if (retrieved == null || retrieved.isEmpty()) break
                ct += retrieved.size
                pb()
                pl("Retrieving messages from #$name in ${guild.name}: $ct messages")
            }
            val h = mh.retrievedHistory.filter { it.author == jda.selfUser }
            pb()
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
                print("No token found in preferences, please enter one: ")
                var t = readLine()
                while (t.isNullOrBlank()) {
                    t = readLine()
                }
                token = t
            } else {
                print("Token found in preferences, would you like to use it? (Y/n) ")
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
                                prefs.put("token", token)
                                prefs.flush()
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
