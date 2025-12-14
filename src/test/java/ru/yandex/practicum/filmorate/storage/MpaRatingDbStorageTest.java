package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.db.MpaRatingDbStorage;
import ru.yandex.practicum.filmorate.storage.db.mapper.MpaRatingRowMapper;

import java.util.Collection;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@Import({MpaRatingDbStorage.class, MpaRatingRowMapper.class})
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class MpaRatingDbStorageTest {
    private final MpaRatingDbStorage mpaRatingDbStorage;

    @Test
    public void shouldFindMpaById() {
        MpaRating mpaRating = mpaRatingDbStorage.getMpaById(1);

        assertThat(mpaRating).hasFieldOrPropertyWithValue("name", "G");
    }

    @Test
    public void shouldFindAllMpa() {
        Collection<MpaRating> mpaRatings = mpaRatingDbStorage.getAllMpa();

        assertThat(mpaRatings).isNotNull()
                .hasSize(5)
                .extracting(MpaRating::getName)
                .contains("G", "PG", "PG-13", "R", "NC-17");
    }
}
