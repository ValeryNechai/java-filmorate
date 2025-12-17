package ru.yandex.practicum.filmorate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.Collection;

@RestController
@RequestMapping
public class FilmController {
    private final FilmService filmService;

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PostMapping("/films")
    public Film createFilm(@RequestBody Film film) {
        return filmService.createFilm(film);
    }

    @PutMapping("/films")
    public Film updateFilm(@RequestBody Film newFilm) {
        return filmService.updateFilm(newFilm);
    }

    @DeleteMapping("/films/{filmId}")
    public ResponseEntity<Void> deleteFilm(@PathVariable Long filmId) {
        filmService.deleteFilm(filmId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/films")
    public Collection<Film> getAllFilms() {
        return filmService.getAllFilms();
    }

    @GetMapping("/films/{id}")
    public Film getFilm(@PathVariable Long id) {
        return filmService.getFilm(id);
    }

    @PutMapping("/films/{id}/like/{userId}")
    public void addLike(@PathVariable Long id, @PathVariable Long userId) {
        filmService.addLike(id, userId);
    }

    @DeleteMapping("/films/{id}/like/{userId}")
    public void deleteLike(@PathVariable Long id, @PathVariable Long userId) {
        filmService.deleteLike(id, userId);
    }

    @GetMapping("/films/popular")
    public Collection<Film> getPopularFilms(@RequestParam(defaultValue = "1000") int count,
                                            @RequestParam(required = false) Integer genreId,
                                            @RequestParam(required = false) Integer year) {
        return filmService.getPopularFilms(count, genreId, year);
    }

    @GetMapping("/films/common")
    public Collection<Film> getCommonFilms(@RequestParam Long userId,
                                           @RequestParam Long friendId) {
        return filmService.getCommonFilms(userId, friendId);
    }

    @GetMapping("/genres")
    public Collection<Genre> getAllGenres() {
        return filmService.getAllGenres();
    }

    @GetMapping("/genres/{id}")
    public Genre getGenre(@PathVariable Integer id) {
        return filmService.getGenreById(id);
    }

    @GetMapping("/mpa")
    public Collection<MpaRating> getAllMpa() {
        return filmService.getAllMpa();
    }

    @GetMapping("/mpa/{id}")
    public MpaRating getMpa(@PathVariable Integer id) {
        return filmService.getMpaById(id);
    }

    @PostMapping("/reviews")
    public Review createReview(@RequestBody Review review) {
        return filmService.createReview(review);
    }

    @PutMapping("/reviews")
    public Review updateReview(@RequestBody Review newReview) {
        return filmService.updateReview(newReview);
    }

    @DeleteMapping("/reviews/{id}")
    public void deleteReview(@PathVariable Long id) {
        filmService.deleteReview(id);
    }

    @GetMapping("/reviews/{id}")
    public Review getReviewById(@PathVariable Long id) {
        return filmService.getReviewById(id);
    }

    @GetMapping("/reviews")
    public Collection<Review> getReviewsByFilmIdAndCount(@RequestParam Long filmId,
                                                         @RequestParam(defaultValue = "10") int count) {
        return filmService.getReviewsByFilmIdAndCount(filmId, count);
    }

    @GetMapping("/films/director/{directorId}")
    public Collection<Film> getFilmsByDirector(@PathVariable Long directorId,
                                               @RequestParam String sortBy) {
        return filmService.getFilmsByDirector(directorId, sortBy);
    }

    @PutMapping("/reviews/{id}/like/{userId}")
    public void addLikeToReview(@PathVariable Long id, @PathVariable Long userId) {
        filmService.addLikeToReview(id, userId);
    }

    @PutMapping("/reviews/{id}/dislike/{userId}")
    public void addDislikeToReview(@PathVariable Long id, @PathVariable Long userId) {
        filmService.addDislikeToReview(id, userId);
    }

    @DeleteMapping("/reviews/{id}/like/{userId}")
    public void deleteLikeFromReview(@PathVariable Long id, @PathVariable Long userId) {
        filmService.deleteLikeFromReview(id, userId);
    }

    @DeleteMapping("/reviews/{id}/dislike/{userId}")
    public void deleteDislikeFromReview(@PathVariable Long id, @PathVariable Long userId) {
        filmService.deleteDislikeFromReview(id, userId);
    }
}
