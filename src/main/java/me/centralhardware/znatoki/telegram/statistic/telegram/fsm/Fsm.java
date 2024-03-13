package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder;
import me.centralhardware.znatoki.telegram.statistic.eav.Property;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Organization;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.TelegramUser;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.UserMapper;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.ReplyKeyboardBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public abstract class Fsm {

    @Autowired
    protected Storage storage;
    @Autowired
    protected TelegramSender sender;
    @Autowired
    private OrganizationMapper organizationMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    protected TelegramUtil telegramUtil;

    abstract void process(Update update);
    abstract boolean isActive(Long chatId);

    protected Optional<User> getLogUser(Long userId){
        return Optional.ofNullable(userMapper.getById(userId))
                .map(TelegramUser::getOrganizationId)
                .map(it -> organizationMapper.getById(it))
                .map(Organization::getLogChatId)
                .map(logChatId -> {
                    var logUser = new User();
                    logUser.setId(logChatId);
                    logUser.setLanguageCode("ru");
                    return logUser;
                });
    }

    protected void processCustomProperties(Update update, PropertiesBuilder builder, Consumer<List<Property>> onFinish) {
        var user = TelegramUtil.getFrom(update);
        Long chatId = telegramUtil.getUserId(update);
        builder
                .validate(update)
                .toEither()
                .peekLeft(error -> sender.sendText(error, user))
                .peek(it -> {
                    if (!builder.setProperty(update)) {
                        sender.sendText("произошла ошибка. Можете попробовать снова", user);
                        storage.remove(chatId);
                        return;
                    }

                    builder.getNext()
                            .ifPresentOrElse(
                                    next -> {
                                        if (!next.getRight().isEmpty()) {
                                            var keyboard = ReplyKeyboardBuilder
                                                    .create()
                                                    .setText(next.getLeft());
                                            next.getRight().forEach(variant -> keyboard.row()
                                                    .button(variant)
                                                    .endRow());
                                            sender.send(keyboard.build(chatId));
                                        } else {
                                            sender.sendText(next.getLeft(), user);
                                        }
                                    },
                                    () -> onFinish.accept(builder.getProperties()));
                });
    }

}
