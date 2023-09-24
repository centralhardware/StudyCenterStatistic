package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.entity.Service;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.redis.dto.Role;
import me.centralhardware.znatoki.telegram.statistic.redis.dto.ZnatokiUser;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.ReplyKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.steps.AddOrganization;
import me.centralhardware.znatoki.telegram.statistic.utils.Transcriptor;
import me.centralhardware.znatoki.telegram.statistic.validate.ServiceValidator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Objects;
import java.util.Optional;


@Component
@RequiredArgsConstructor
public class OrganizationFsm extends Fsm {

    private final TelegramUtil telegramUtil;
    private final Redis redis;
    private final Transcriptor transcriptor;
    private final ServiceValidator serviceValidator;

    private final OrganizationMapper organizationMapper;
    private final ServicesMapper servicesMapper;

    @Override
    public void process(Update update) {
        Long userId = telegramUtil.getUserId(update);
        String text = Optional.of(update)
                .map(Update::getMessage)
                .map(Message::getText)
                .orElse(null);

        User user = telegramUtil.getFrom(update);
        switch (storage.getOrganizationStage(userId)){
            case ADD_NAME -> {
                if (StringUtils.isBlank(text)){
                    sender.sendText("Введите имя организации", user);
                    return;
                }

                storage.getOrganization(userId).setName(text);
                storage.setOrganizationStage(userId, AddOrganization.ADD_SERVICES);
                sender.sendText("Введите название оказываемых услуг. /complete для завершения.", user);
            }
            case ADD_SERVICES -> {
                if (StringUtils.isBlank(text)){
                    sender.sendText("Введите название услуги", user);
                    return;
                }

                if (Objects.equals(text, "/complete")){
                    storage.setOrganizationStage(userId, AddOrganization.ADD_OWNER_SUBJECT);

                    var builder = ReplyKeyboardBuilder
                            .create()
                                    .setText("Введите услуги, которые будет оказывать вы лично. /complete для завершения ввода.");

                    storage.getOrganization(userId).getServices()
                                    .forEach(service -> {
                                        builder.row()
                                                .button(service)
                                                .endRow();
                                    });

                    sender.send(builder.build(userId), user);
                    return;
                }

                storage.getOrganization(userId).getServices().add(text);

                sender.sendText("Услуга сохранена", user);
                return;
            }
            case ADD_OWNER_SUBJECT -> {
                if (Objects.equals(text, "/complete")){
                    var org = storage.getOrganization(userId);
                    org.setOwner(userId);
                    organizationMapper.insert(org);

                    org.getServices()
                            .forEach(service -> {
                                var s = new Service();
                                s.setOrgId(org.getId());
                                s.setName(service);
                                s.setKey(transcriptor.convert(service));
                                servicesMapper.insert(s);
                            });

                    var ownServices = org.getOwnerServices()
                            .stream()
                            .map(it -> servicesMapper.getServiceId(org.getId(), it))
                            .toList();

                    var znatokiUser = ZnatokiUser.builder()
                            .organizationId(org.getId())
                            .role(Role.ADMIN)
                            .services(ownServices)
                            .build();

                    redis.put(user.getId().toString(), znatokiUser);

                    storage.remove(userId);

                    sender.sendText("""
                        Организация создана. 
                        Добавьте клиентов через /addPupil.
                        Заносите услуги и добавляйте оплату /addTime /addPayment
                        """, user);
                    return;
                }

                if (storage.getOrganization(userId).getServices().contains(text)){
                    sender.sendText("Сохранено", user);
                    storage.getOrganization(userId).getOwnerServices().add(text);
                } else {
                    sender.sendText("Введите значение использую клавиатуру", user);
                }
            }
        }
    }

    @Override
    public boolean isActive(Long chatId) {
        return storage.containsOrganization(chatId);
    }
}
