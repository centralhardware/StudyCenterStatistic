package me.centralhardware.znatoki.telegram.statistic.entity

import java.time.LocalDateTime
import java.util.*
import kotliquery.Row
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.toProperties

class Service(
    val id: UUID,
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val chatId: Long,
    val serviceId: Long,
    val clientId: Int,
    val amount: Int,
    val properties: List<Property>,
)

fun Row.parseTime() =
    Service(
        uuid("id"),
        localDateTime("date_time"),
        long("chat_id"),
        long("service_id"),
        int("pupil_id"),
        int("amount"),
        string("properties").toProperties(),
    )

class ServiceBuilder : Builder {
    var id: UUID = UUID.randomUUID()
    var chatId: Long? = null
    var serviceId: Long? = null
    var amount: Int? = null
    var properties: List<Property>? = null
    var propertiesBuilder: PropertiesBuilder? = null
    var clientIds: MutableSet<Int> = mutableSetOf()

    fun nextProperty() = propertiesBuilder!!.next()

    fun build(): List<Service> =
        clientIds.map {
            Service(
                id = id,
                chatId = chatId!!,
                serviceId = serviceId!!,
                clientId = it,
                amount = amount!!,
                properties = propertiesBuilder!!.properties.toList(),
            )
        }
}

fun Collection<Service>.toClientIds(): List<Int> = this.map { it.clientId }.toList()
