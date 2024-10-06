package me.centralhardware.znatoki.telegram.statistic

import com.sun.net.httpserver.HttpServer
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.behaviour_builder.createSubContextAndDoAsynchronouslyWithUpdatesFilter
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.types.BotCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic.paymentDeleteCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic.paymentRestoreCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic.timeDeleteCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.statistic.timeRestoreCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student.deleteUserCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler.student.userInfoCallback
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.debug.dailyReportCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.organization.grafanaCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.resetCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.startCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand.addPaymentCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand.addTimeCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand.reportCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand.reportPreviousCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand.addClientCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand.searchCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand.userInfoCommand
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import me.centralhardware.znatoki.telegram.statistic.telegram.processInline
import me.centralhardware.znatoki.telegram.statistic.report.dailyReport
import me.centralhardware.znatoki.telegram.statistic.report.monthReport
import me.centralhardware.znatoki.telegram.statistic.telegram.KtorPipelineStepsHolder
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

val log = LoggerFactory.getLogger("bot")
lateinit var bot: TelegramBot
suspend fun main() {
    HttpServer.create().apply { bind(InetSocketAddress(80), 0); createContext("/health") {
        if (KtorPipelineStepsHolder.health) {
            it.sendResponseHeaders(200, 0);
        } else {
            it.sendResponseHeaders(400, 0);
        }
        it.responseBody.close()
    }; start() }
    ClientService.init()
    GlobalScope.launch {
        monthReport()
    }
    GlobalScope.launch {
        dailyReport()
    }
    val res = telegramBotWithBehaviourAndLongPolling(
        Config.Telegram.token,
        defaultExceptionsHandler = { log.info("", it) },
        scope = CoroutineScope(Dispatchers.IO),
        builder = { pipelineStepsHolder = KtorPipelineStepsHolder() }
    ) {
        setDefaultKSLog(
            KSLog("ZnatokiStatistic", minLoggingLevel = LogLevel.INFO)
        )
        setMyCommands(
            BotCommand("addtime", "Добавить запись"),
            BotCommand("addpayment", "Добавить оплату"),
            BotCommand("report", "Отчет за текущий месяц"),
            BotCommand("reportprevious", "Отчет за предыдущий месяц"),
            BotCommand("reset", "Сбросить состояние")
        )

        onCommand("start") { startCommand(it) }
        onCommand("reset") { resetCommand(it) }

        onContentMessage({ Storage.contain(it.userId()) }) {
            Storage.process(it)
        }



        createSubContextAndDoAsynchronouslyWithUpdatesFilter(
            updatesUpstreamFlow = allUpdatesFlow.filter { UserMapper.hasWriteRight(it.userId()) }) {
            onCommand(Regex("addPupil|addpupil")) { addClientCommand(it) }
        }

        createSubContextAndDoAsynchronouslyWithUpdatesFilter(
            updatesUpstreamFlow = allUpdatesFlow.filter { UserMapper.hasReadRight(it.userId()) }) {
            onCommandWithArgs("i") { message, args -> userInfoCommand(message, args) }
            onCommandWithArgs("s") { message, args -> searchCommand(message, args) }
            onCommand("report") { reportCommand(it) }
            onCommand(Regex("reportPrevious|reportprevious")) { reportPreviousCommand(it) }
            onCommand(Regex("addTime|addtime")) { addTimeCommand(it) }
            onCommand(Regex("addPayment|addpayment")) { addPaymentCommand(it) }

            onDataCallbackQuery(Regex("user_info\\d+\$")) { userInfoCallback(it) }

            onBaseInlineQuery { processInline(it) }
        }

        createSubContextAndDoAsynchronouslyWithUpdatesFilter(
            updatesUpstreamFlow = allUpdatesFlow.filter { UserMapper.hasAdminRight(it.userId()) }) {
            onCommand("grafana") { grafanaCommand(it) }
            onCommandWithArgs("dailyReport") { message, args -> dailyReportCommand(message, args) }

            onDataCallbackQuery(Regex("delete_user\\d+\$")) { deleteUserCallback(it) }
            onDataCallbackQuery(Regex("timeRestore-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                timeRestoreCallback(it)
            }
            onDataCallbackQuery(Regex("timeDelete-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                timeDeleteCallback(it)
            }
            onDataCallbackQuery(Regex("paymentRestore-\\d+\$")) {
                paymentRestoreCallback(it)
            }
            onDataCallbackQuery(Regex("paymentDelete-\\d+\$")) {
                paymentDeleteCallback(it)
            }
        }

    }
    bot = res.first
    res.second.join()

}