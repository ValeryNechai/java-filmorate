package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface FilmStorage {
    Film createFilm(Film film);

    Film updateFilm(Film newFilm);

    Collection<Film> getAllFilms();

    Film getFilm(Long id);

    Collection<Film> getPopularFilms(int count, Integer genreId, Integer year);

    Collection<Film> getCommonFilms(Long userId, Long friendId);

    boolean existsById(Long id);

    List<Film> searchFilms(String query, String by);

    Collection<Film> getFilmsByDirector(Long directorId, String sortBy);

    Collection<Film> getRecommendations(Long userId);

    void deleteFilmById(Long filmId);

    Map<Long, Set<Director>> getDirectorsByFilmIds(Set<Long> filmIds);

    Set<Director> getDirectorsByFilmId(Long filmId);
}
