package ru.yandex.practicum.filmorate.storage.inMemory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();
    private final Set<Genre> genres = Set.of(
            new Genre(1, "Комедия"),
            new Genre(2, "Драма"),
            new Genre(3, "Мультфильм"),
            new Genre(4, "Триллер"),
            new Genre(5, "Документальный"),
            new Genre(6, "Боевик")
    );
    private final Set<MpaRating> mpaRatings = Set.of(
            new MpaRating(1, "G"),
            new MpaRating(2, "PG"),
            new MpaRating(3, "PG-13"),
            new MpaRating(4, "R"),
            new MpaRating(5, "NC-17")
    );
    private static long id = 0;

    @Override
    public Film createFilm(Film film) {
        film.setId(++id);
        films.put(film.getId(), film);
        log.debug("Фильм {} успешно добавлен", film.getName());
        return film;
    }

    @Override
    public Film updateFilm(Film newFilm) {
        if (films.containsKey(newFilm.getId())) {
            films.put(newFilm.getId(), newFilm);
            log.debug("Фильм с id {} успешно обновлен.", newFilm.getId());
            return newFilm;
        }

        log.warn("Фильм с id = {} не найден", newFilm.getId());
        throw new NotFoundException("Фильм с id = " + newFilm.getId() + " не найден");
    }

    @Override
    public Collection<Film> getAllFilms() {
        return films.values();
    }

    @Override
    public Film getFilm(Long id) {
        if (films.containsKey(id)) {
            return films.get(id);
        } else {
            log.warn("Фильм с id = {} не найден", id);
            throw new NotFoundException("Фильм с id = " + id + " не найден");
        }
    }

    @Override
    public Collection<Film> getPopularFilms(int count) {
        return getAllFilms()
                .stream()
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Genre> getAllGenres() {
        return films.values().stream()
                .flatMap(film -> film.getFilmGenres().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public Genre getGenreById(int id) {
        return genres.stream()
                .filter(genre -> genre.getId() == id)
                .findFirst().get();
    }

    @Override
    public Collection<MpaRating> getAllMpa() {
        return films.values().stream()
                .map(Film::getMpaRating)
                .collect(Collectors.toSet());
    }

    @Override
    public MpaRating getMpaById(int id) {
        return mpaRatings.stream()
                .filter(mpa -> mpa.getId() == id)
                .findFirst().get();
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null || !(films.containsKey(id))) {
            return false;
        }

        return true;
    }
}
