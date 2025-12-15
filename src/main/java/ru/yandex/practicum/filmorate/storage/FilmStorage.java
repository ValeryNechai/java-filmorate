package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.Collection;
import java.util.List;

public interface FilmStorage {
    Film createFilm(Film film);

    Film updateFilm(Film newFilm);

    Collection<Film> getAllFilms();

    Film getFilm(Long id);

    List<Film> searchFilms(String query, String by);

    Collection<Film> getPopularFilms(int count);

    Collection<Genre> getAllGenres();

    Genre getGenreById(int id);

    Collection<MpaRating> getAllMpa();

    MpaRating getMpaById(int id);

    boolean existsById(Long id);
}
