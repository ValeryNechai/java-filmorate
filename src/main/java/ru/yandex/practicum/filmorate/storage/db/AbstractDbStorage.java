package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import ru.yandex.practicum.filmorate.exception.InternalServerException;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractDbStorage<T> {
    protected final JdbcTemplate jdbc;
    protected final RowMapper<T> mapper;

    protected Optional<T> findOne(String query, Object... params) {
        log.debug("Выполнение поиска одной записи. SQL: {}", query);
        log.trace("Параметры запроса: {}", Arrays.toString(params));
        try {
            T result = jdbc.queryForObject(query, mapper, params);
            return Optional.ofNullable(result);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Запись не найдена (EmptyResultDataAccessException).");
            return Optional.empty();
        }
    }

    protected List<T> findMany(String query, Object... params) {
        log.debug("Выполнение поиска нескольких записей. SQL: {}", query);
        log.trace("Параметры запроса: {}", Arrays.toString(params));
        return jdbc.query(query, mapper, params);
    }

    protected long insert(String query, Object... params) {
        log.debug("Выполнение вставки записи. SQL: {}", query);
        log.trace("Параметры вставки: {}", Arrays.toString(params));
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps; }, keyHolder);
        Long id = keyHolder.getKeyAs(Long.class);

        if (id != null) {
            log.debug("Запись успешно сохранена.");
            return id;
        } else {
            log.error("Не удалось сохранить данные.");
            throw new InternalServerException("Не удалось сохранить данные.");
        }
    }

    protected void update(String query, Object... params) {
        log.debug("Выполнение обновления записи. SQL: {}", query);
        log.trace("Параметры обновления: {}", Arrays.toString(params));
        int updatedRows = jdbc.update(query, params);
        if (updatedRows == 0) {
            log.error("Ошибка при обновлении записи.");
            throw new InternalServerException("Не удалось обновить данные");
        }
    }
}
