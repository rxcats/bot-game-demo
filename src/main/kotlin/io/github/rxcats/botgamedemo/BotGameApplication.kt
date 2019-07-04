package io.github.rxcats.botgamedemo

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.util.concurrent.ThreadLocalRandom

@SpringBootApplication
class BotGameApplication

fun main(args: Array<String>) {
    ApiContextInitializer.init()
    runApplication<BotGameApplication>(*args)
}

data class Player(var name: String, var hp: Long = 1000)
data class Enemy(var name: String, var hp: Long = 1000)

@Component
class GameBot(val player: Player = Player("유저"), val enemy: Enemy = Enemy("Boss 똔코치")) : TelegramLongPollingBot() {
    companion object {
        const val EMOJI_WIN = "\uD83D\uDE06"
        const val EMOJI_LOSE = "\uD83D\uDE1E"
        const val EMOJI_USER = "\uD83E\uDDD1"
        const val EMOJI_ENEMY = "\uD83D\uDC07"
        const val EMOJI_DAMAGE = "\uD83C\uDFF9"
        const val EMOJI_GAMEPAD = "\uD83C\uDFAE"
    }

    @Value("\${bot.token}")
    private lateinit var token: String

    @Value("\${bot.name}")
    private lateinit var botName: String

    @Value("\${bot.chat-id}")
    private lateinit var chatId: String

    override fun getBotUsername(): String = botName

    override fun getBotToken(): String = token

    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage()) {
            if (update.message.text == "시작" || update.message.text == "start") {
                gameStart()
            }

            if (update.message.text.startsWith("이름") || update.message.text.startsWith("name")) {
                val cmd = update.message.text.split(" ")
                if (cmd.size == 2 && !cmd[1].isBlank()) {
                    player.name = cmd[1]
                    sendStatus()
                }
            }

            if (update.message.text == "공격" || update.message.text == "attack") {
                if (isGameEnded(true)) return

                attackPlayer()
                if (!isGameEnded(false)) {
                    attackEnemy()
                }

                checkGameEnd()
            }
        }
    }

    fun sendMessage(text: String) {
        val msg = SendMessage().setChatId(chatId.toLong()).setText(text)
        try {
            execute(msg)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    fun gameStart() {
        player.hp = 1000
        enemy.hp = 1000
        sendMessage("$EMOJI_GAMEPAD 게임시작 $EMOJI_GAMEPAD")
        sendStatus()
    }

    fun sendStatus(damage: Long? = null, title: String? = null) {
        if (damage == null) {
            sendMessage("[${player.name}] ${player.hp}\n[${enemy.name}] ${enemy.hp}")
        } else {
            sendMessage("$EMOJI_DAMAGE $title=$damage\n[${player.name}] ${player.hp}\n[${enemy.name}] ${enemy.hp}")
        }
    }

    private fun damage() = ThreadLocalRandom.current().nextLong(10, 999)

    fun attackPlayer() {
        val d = damage()
        enemy.hp -= d
        sendStatus(d, "$EMOJI_USER ${player.name} 공격")
    }

    fun attackEnemy() {
        val d = damage()
        player.hp -= d
        sendStatus(d, "$EMOJI_ENEMY ${enemy.name} 공격")
    }

    fun isGameEnded(alert: Boolean): Boolean {
        if (player.hp <= 0 || enemy.hp <= 0) {
            if (alert) {
                sendMessage("이미 게임이 종료 되었습니다. 다시 시작해 주세요")
            }
            return true
        }
        return false
    }

    fun checkGameEnd() {
        if (player.hp <= 0) {
            sendMessage("[${player.name}] ${player.hp}\n[${enemy.name}] ${enemy.hp}\nYou Lose. $EMOJI_LOSE")
            return
        }

        if (enemy.hp <= 0) {
            sendMessage("[${player.name}] ${player.hp}\n[${enemy.name}] ${enemy.hp}\nYou Win. $EMOJI_WIN")
        }
    }
}