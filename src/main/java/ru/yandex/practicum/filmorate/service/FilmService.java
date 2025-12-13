package ru.yandex.practicum.filmorate.service;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.Collection;

public interface FilmService {

    Film createFilm(Film film);

    Film updateFilm(Film newFilm);

    Collection<Film> getAllFilms();

    Film getFilm(Long id);

    void addLike(Long id, Long userId);

    void deleteLike(Long id, Long userId);

    Collection<Film> getPopularFilms(int count);

    Collection<Film> getCommonFilms(Long userId, Long friendId);

    Collection<Genre> getAllGenres();

    Genre getGenreById(int id);

    Collection<MpaRating> getAllMpa();

    MpaRating getMpaById(int id);
}
