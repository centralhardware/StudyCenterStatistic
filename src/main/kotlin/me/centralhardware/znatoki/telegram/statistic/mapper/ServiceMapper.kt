package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.*
import me.centralhardware.znatoki.telegram.statistic.entity.Service
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

@Component
class ServiceMapper(private val session: Session) {

    fun getOrgId(id: UUID): UUID? = session.run(
        queryOf(
            """
            SELECT org_id
            FROM service
            WHERE id = :id
            LIMIT 1
            """, mapOf("id" to id)
        ).map { row -> row.uuid("org_id") }.asSingle
    )

    fun insertTime(service: Service) = session.execute(
        queryOf(
            """
            INSERT INTO service (
                date_time,
                id,
                chat_id,
                service_id,
                amount,
                pupil_id,
                org_id,
                properties
            ) VALUES (
                :dateTime,
                :id,
                :chatId,
                :serviceId,
                :amount,
                :clientId,
                :organizationId,
                :properties::JSONB
            )
            """, mapOf(
                "dateTime" to service.dateTime,
                "id" to service.id,
                "chatId" to service.chatId,
                "serviceId" to service.serviceId,
                "amount" to service.amount,
                "clientId" to service.clientId,
                "organizationId" to service.organizationId,
                "properties" to service.properties.toJson()
            )
        )
    )

    val servicesMapper: (Row) -> Service = { row ->
        Service(
            row.uuid("id"),
            row.localDateTime("date_time"),
            row.long("chat_id"),
            row.long("service_id"),
            row.int("pupil_id"),
            row.int("amount"),
            row.uuid("org_id"),
            row.string("properties").toProperties(),
        )
    }

    fun getTimes(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<Service> = session.run(
        queryOf(
            """
            SELECT id,
                   date_time,
                   chat_id,
                   service_id,
                   pupil_id,
                   amount,
                   org_id,
                   properties
            FROM service
            WHERE chat_id = :userId
                AND date_time between :startDate and :endDate
                AND is_deleted=false
            """, mapOf(
                "userId" to userId,
                "startDate" to startDate,
                "endDate" to endDate
            )
        ).map(servicesMapper).asList
    )

    fun getTodayTimes(chatId: Long): List<Service> {
        return getTimes(chatId, LocalDateTime.now().with(LocalTime.MIN), LocalDateTime.now())
    }

    fun getCuurentMontTimes(chatId: Long): List<Service> {
        return getTimes(
            chatId,
            LocalDateTime.now().startOfMonth(),
            LocalDateTime.now().endOfMonth()
        )
    }

    fun getPrevMonthTimes(chatId: Long): List<Service> {
        return getTimes(
            chatId,
            LocalDateTime.now().prevMonth().startOfMonth(),
            LocalDateTime.now().prevMonth().endOfMonth()
        )
    }

    fun getIds(orgId: UUID): List<Long> = session.run(
        queryOf(
            """
            SELECT DISTINCT chat_id
            FROM service
            WHERE is_deleted = false AND org_id = :org_id
            """, mapOf("org_id" to orgId)
        ).map { row -> row.long("chat_id") }.asList
    )

    fun setDeleted(timeId: UUID, isDeleted: Boolean) = session.run(
        queryOf(
            """
            UPDATE service
            SET is_deleted = :is_deleted
            WHERE id = :id
            """, mapOf(
                "id" to timeId,
                "is_deleted" to isDeleted
            )
        ).asUpdate
    )

    fun getServicesForCLient(id: Int): List<Long> = session.run(
        queryOf(
            """
            SELECT DISTINCT service_id
            FROM service
            WHERE pupil_id = :id ANd is_deleted=false
            """, mapOf("id" to id)
        ).map { row -> row.long("service_id") }.asList
    )

}