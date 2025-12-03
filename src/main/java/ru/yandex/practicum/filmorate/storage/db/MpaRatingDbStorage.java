package ru.yandex.practicum.filmorate.storage.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.Collection;

@Repository
public class MpaRatingDbStorage extends AbstractDbStorage<MpaRating> implements MpaRatingStorage {
    private static final String FIND_ALL_QUERY = "SELECT * FROM MPA_RATINGS";
    private static final String FIND_BY_ID_QUERY = "SELECT * FROM MPA_RATINGS WHERE RATING_ID = ?";

    public MpaRatingDbStorage(JdbcTemplate jdbc, RowMapper<MpaRating> mapper) {
        super(jdbc, mapper);
    }

    @Override
    public MpaRating getMpaById(Integer id) {
        return findOne(FIND_BY_ID_QUERY, id)
                .orElseThrow(() -> new NotFoundException("Рейтинг с id = " + id + " не найден"));
    }

    @Override
    public Collection<MpaRating> getAllMpa() {
        return findMany(FIND_ALL_QUERY);
    }
}
