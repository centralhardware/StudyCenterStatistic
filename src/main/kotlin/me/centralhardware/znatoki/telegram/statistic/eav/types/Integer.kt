package me.centralhardware.znatoki.telegram.statistic.eav.types

import arrow.core.Either
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import me.centralhardware.znatoki.telegram.statistic.eav.types.Type.Companion.OPTIONAL_TEXT
import org.apache.commons.lang3.StringUtils

typealias NumberType = Integer

data object Integer : Type {

    override fun format(name: String, isOptional: Boolean): String {
        return "Введите $name (число). ${if (isOptional) OPTIONAL_TEXT else ""}"
    }

    override fun validate(
        message: CommonMessage<MessageContent>,
        variants: List<String>
    ): Either<String, Unit> {
        return if (StringUtils.isNumeric(message.text)) Either.Right(Unit)
        else Either.Left("Введите число")
    }
}
