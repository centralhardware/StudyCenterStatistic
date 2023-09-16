package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.Storage;
import me.centralhardware.znatoki.telegram.statistic.entity.Enum.HowToKnow;
import me.centralhardware.znatoki.telegram.statistic.entity.Enum.Subject;
import me.centralhardware.znatoki.telegram.statistic.entity.Pupil;
import me.centralhardware.znatoki.telegram.statistic.i18n.ErrorConstant;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.service.PupilService;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.ReplyKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.utils.TelephoneUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
@Slf4j
public class PupilFsm implements Fsm{

    private final Storage storage;
    private final TelegramService telegramService;
    private final PupilService pupilService;
    private final ResourceBundle resourceBundle;
    private final TelegramSender sender;

    @Override
    public void process(Update update) {
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        var user = update.getMessage().getFrom();

        if (telegramService.isUnauthorized(chatId) || !telegramService.hasWriteRight(chatId)){
            sender.sendMessageFromResource(ErrorConstant.ACCESS_DENIED, user);
            return;
        }

        switch (storage.getPupilStage(chatId)){
            case INPUT_FIO -> {
                if (checkCancel(text, user)) return;

                String[] words = text.split(" ");
                if (!(words.length >= 2 && words.length <= 3)) {
                    sender.sendMessageFromResource(MessageConstant.INPUT_FIO_REQUIRED_FORMAT, user);
                    return;
                }
                me.centralhardware.znatoki.telegram.statistic.entity.Pupil pupil = storage.getPupil(chatId);
                if (words.length == 3){
                    pupil.setSecondName(words[0]);
                    pupil.setName(words[1]);
                    pupil.setLastName(words[2]);
                } else {
                    pupil.setSecondName(words[0]);
                    pupil.setName(words[1]);
                    pupil.setLastName("");
                }
                if (pupilService.checkExistenceByFio(pupil.getName(), pupil.getSecondName(), pupil.getLastName())){
                    sender.sendMessageFromResource(MessageConstant.FIO_ALREADY_IN_DATABASE, user);
                    return;
                }
                next(chatId);
                sender.sendMessageFromResource(MessageConstant.INPUT_CLASS, user);
            }
            case INPUT_CLASS_NUMBER -> {
                if (checkCancel(text, user)) return;

                if (text.equals(SKIP_COMMAND)){
                    getPupil(chatId).setClassNumber(null);
                    sender.sendMessageFromResource(MessageConstant.INPUT_DATE_IN_FORMAT, user);
                    next(chatId);
                    return;
                }

                if (StringUtils.isNumeric(text) || text.equals("-1")){
                    int classNumber = Integer.parseInt(text);
                    if (classNumber > 11 || classNumber < 1 && classNumber != -1){
                        sender.sendMessageFromResource(MessageConstant.CLASS_MUST_BE_IN_RANGE, user);
                        return;
                    }
                    getPupil(chatId).setClassNumber(classNumber);
                    sender.sendMessageFromResource(MessageConstant.INPUT_DATE_IN_FORMAT, user);
                    next(chatId);
                } else {
                    sender.sendMessageFromResource(MessageConstant.NECESSARY_TO_INPUT_NUMBER, user);
                }
            }
            case INPUT_DATE_OF_RECORD -> {
                if (checkCancel(text, user)) return;

                LocalDate date;
                try {
                    date = LocalDate.parse(text, dateFormat);
                } catch (DateTimeException e){
                    sender.sendMessageFromResource(MessageConstant.DATE_FORMAT_ERROR, user);
                    return;
                }

                if (date.getYear() < 119){
                    sender.sendMessageFromResource(MessageConstant.YEAR_OF_RECORD_LOW_THEN_2019, user);
                    return;
                }
                getPupil(chatId).setDateOfRecord(date.atStartOfDay());
                next(chatId);
                sender.sendMessageFromResource(MessageConstant.INPUT_DATE_OF_BIRTH_IN_FORMAT, user);
            }
            case INPUT_DATE_OF_BIRTH -> {
                if (checkCancel(text, user)) return;

                LocalDate date;
                try {
                    date = LocalDate.parse(text, dateFormat);
                } catch (DateTimeException e){
                    sender.sendMessageFromResource(MessageConstant.DATE_FORMAT_ERROR, user);
                    return;
                }
                getPupil(chatId).setDateOfBirth(date.atStartOfDay());
                next(chatId);
                sender.sendMessageFromResource(MessageConstant.INPUT_PUPIL_TEL, user);
            }
            case INPUT_TELEPHONE -> {
                if (checkCancel(text, user)) return;

                if (text.equals(SKIP_COMMAND)){
                    next(chatId);
                    getPupil(chatId).setTelephone("");
                    sender.sendMessageFromResource(MessageConstant.INPUT_RESPONSIBLE_TEL, user);
                    return;
                }
                if (TelephoneUtils.validate(text)){
                    if (pupilService.existByTelephone(text)){
                        sender.sendMessageFromResource(MessageConstant.TEL_ALREADY_EXIST, user);
                        return;
                    }
                    getPupil(chatId).setTelephone(text);
                    next(chatId);
                    sender.sendMessageFromResource(MessageConstant.INPUT_RESPONSIBLE_TEL, user);
                } else {
                    sender.sendMessageFromResource(MessageConstant.INPUT_RIGHT_TEL_NUMBER, user);
                }
            }
            case INPUT_TELEPHONE_OF_RESPONSIBLE -> {
                if (checkCancel(text, user)) return;

                if (text.equals(SKIP_COMMAND)){
                    next(chatId);
                    ReplyKeyboardBuilder replyKeyboardBuilder = ReplyKeyboardBuilder.
                            create().
                            setText(resourceBundle.getString("INPUT_SUBJECTS"));
                    for (Subject subject : Subject.values()){
                        replyKeyboardBuilder.
                                row().button(subject.getRusName()).endRow();
                    }
                    sender.send(replyKeyboardBuilder.build(chatId), user);
                    return;
                }
                if (pupilService.existByTelephone(text)){
                    sender.sendMessageFromResource(MessageConstant.TEL_ALREADY_EXIST, user);
                    return;
                }
                if (TelephoneUtils.validate(text)){
                    getPupil(chatId).setTelephoneResponsible(text);
                    next(chatId);
                    ReplyKeyboardBuilder replyKeyboardBuilder = ReplyKeyboardBuilder.
                            create().
                            setText(resourceBundle.getString("INPUT_SUBJECTS"));
                    for (Subject subject : Subject.values()){
                        replyKeyboardBuilder.
                                row().button(subject.getRusName()).endRow();
                    }
                    sender.send(replyKeyboardBuilder.build(chatId), user);
                } else {
                    sender.sendMessageFromResource(MessageConstant.INPUT_RIGHT_TEL_NUMBER, user);
                }
            }
            case INPUT_SUBJECT -> {
                if (checkCancel(text, user)) return;

                if (text.equals("/complete")){
                    next(chatId);
                    ReplyKeyboardBuilder replyKeyboardBuilder = ReplyKeyboardBuilder.
                            create().
                            setText(resourceBundle.getString("INPUT_HOW_TO_KNOW"));
                    for (HowToKnow howToKnow : HowToKnow.values()){
                        replyKeyboardBuilder.
                                row().button(howToKnow.getRusName()).endRow();
                    }
                    sender.send(replyKeyboardBuilder.build(chatId), user);
                    return;
                }
                if (Subject.validate(text)){
                    getPupil(chatId).getSubjects().add(Subject.getConstant(text));
                } else {
                    sender.sendMessageFromResource(MessageConstant.SUBJECT_NOT_FOUND, user);
                }
            }
            case INPUT_HOW_TO_KNOW -> {
                if (checkCancel(text, user)) return;

                if (HowToKnow.validate(text)){
                    getPupil(chatId).setHowToKnow(HowToKnow.getConstant(text));
                    next(chatId);
                    sender.sendMessageFromResource(MessageConstant.INPUT_MOTHER_NAME, user);
                } else {
                    sender.sendMessageFromResource(MessageConstant.NOT_FOUND, user);
                }
            }
            case INPUT_MOTHER_NAME -> {
                if (checkCancel(text, user)) return;

                getPupil(chatId).setMotherName(text);

                getPupil(chatId).setCreated_by(chatId);
                sender.sendMessageWithMarkdown(pupilService.save(getPupil(chatId)).toString(), user);
                storage.remove(chatId);
                sender.sendMessageFromResource(MessageConstant.CREATE_PUPIL_FINISHED, user);
            }
        }
    }



    private static final DateTimeFormatter dateFormat    = DateTimeFormatter.ofPattern("dd MM yyyy");

    public Pupil getPupil(Long chatId){
        return storage.getPupil(chatId);
    }

    public void next(Long chatId){
        var next = storage.getPupilStage(chatId).next();
        storage.setPupilStage(chatId, next);
    }


    private static final String CANCEL_COMMAND = "/cancel";
    private static final String SKIP_COMMAND = "/skip";

    private boolean checkCancel(String message, User user) {
        if (message.equals(CANCEL_COMMAND)){
            storage.remove(user.getId());
            sender.sendMessageFromResource(MessageConstant.CANCEL_ADD_PUPIL, user);
            return true;
        } else {
            return false;
        }
    }


    @Override
    public boolean isActive(Long chatId) {
        return storage.containsPupil(chatId);
    }
}