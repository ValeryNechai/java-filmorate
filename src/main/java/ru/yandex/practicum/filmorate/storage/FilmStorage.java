package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.Collection;

public interface FilmStorage {
    Film createFilm(Film film);

    Film updateFilm(Film newFilm);

    Collection<Film> getAllFilms();

    Film getFilm(Long id);

    Collection<Film> getPopularFilms(int count, Integer genreId, Integer year);

    Collection<Film> getCommonFilms(Long userId, Long friendId);

    Collection<Genre> getAllGenres();

    Genre getGenreById(int id);

    Collection<MpaRating> getAllMpa();

    MpaRating getMpaById(int id);

    boolean existsById(Long id);

    Collection<Film> getFilmsByDirector(Long directorId, String sortBy);

    Collection<Film> getRecommendations(Long userId);
}
