package me.centralhardware.znatoki.telegram.statistic.service;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.entity.Pupil;
import me.centralhardware.znatoki.telegram.statistic.entity.Session;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.SessionMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionMapper sessionMapper;

    public String create(Pupil pupil, Long chatId) {
        var session = new Session(pupil, chatId);
        sessionMapper.save(session);
        return session.getUuid();
    }

    public Optional<Session> findByUuid(UUID uuid) {
        return sessionMapper.findByUUid(uuid);
    }

}