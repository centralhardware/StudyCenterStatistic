package me.centralhardware.znatoki.telegram.statistic.telegram.bulider

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo
import kotlin.properties.Delegates

class InlineKeyboardDsl {

    private lateinit var text: String
    private var chatId by Delegates.notNull<Long>()
    private val keyboard: MutableList<List<InlineKeyboardButton>> = mutableListOf()

    fun text(text: String) {
        this.text = text
    }

    fun chatId(chatId: Long) {
        this.chatId = chatId
    }

    fun row(initializer: InlineRow.() -> Unit) {
        keyboard.add(InlineRow().apply(initializer).btns)
    }

    fun build(): SendMessage = SendMessage.builder()
        .text(text)
        .chatId(chatId)
        .replyMarkup(buildReplyMarkup())
        .build()

    fun buildReplyMarkup(): InlineKeyboardMarkup = InlineKeyboardMarkup
        .builder()
        .keyboard(keyboard).build()
}

fun inlineKeyboard(initializer: InlineKeyboardDsl.() -> Unit): InlineKeyboardDsl {
    return InlineKeyboardDsl().apply(initializer)
}

class InlineRow {

    internal val btns: MutableList<InlineKeyboardButton> = mutableListOf()

    fun btn(text: String, callbackData: String) {
        btns.add(InlineKeyboardButton.builder().text(text).callbackData(callbackData).build())
    }

    fun switchToInline() {
        btns.add(InlineKeyboardButton.builder().text("inline").switchInlineQueryCurrentChat("").build())
    }

    fun webApp(url: String, text: String) {
        btns.add(
            InlineKeyboardButton
                .builder()
                .text(text)
                .webApp(
                    WebAppInfo
                        .builder()
                        .url(url)
                        .build()
                )
                .build()
        )
    }

}