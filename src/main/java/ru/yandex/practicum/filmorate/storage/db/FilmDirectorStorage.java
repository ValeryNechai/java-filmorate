package ru.yandex.practicum.filmorate.storage.db;

import ru.yandex.practicum.filmorate.model.Director;

import java.util.Map;
import java.util.Set;

public interface FilmDirectorStorage {
    Set<Director> getDirectorsByFilmId(Long filmId);

    Map<Long, Set<Director>> getDirectorsByFilmIds(Set<Long> filmIds);

    void saveFilmDirectors(Long filmId, Set<Long> directorIds);

    void updateFilmDirectors(Long filmId, Set<Long> directorIds);
}
