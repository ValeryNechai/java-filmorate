package ru.yandex.practicum.filmorate.storage.db.mapper;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class FilmRowMapper implements RowMapper<Film> {
    @Override
    public Film mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(resultSet.getLong("FILM_ID"));
        film.setName(resultSet.getString("FILM_NAME"));
        film.setDescription(resultSet.getString("DESCRIPTION"));
        film.setReleaseDate(resultSet.getDate("RELEASE_DATE").toLocalDate());
        film.setDuration(resultSet.getInt("DURATION"));

        Integer ratingId = resultSet.getObject("RATING_ID", Integer.class);
        String ratingName = resultSet.getString("RATING_NAME");

        if (ratingId != null) {
            MpaRating mpa = new MpaRating();
            mpa.setId(ratingId);
            mpa.setName(ratingName);
            film.setMpaRating(mpa);
        } else {
            MpaRating defaultMpa = new MpaRating();
            defaultMpa.setId(1);
            defaultMpa.setName("G");
            film.setMpaRating(defaultMpa);
        }

        return film;
    }
}
