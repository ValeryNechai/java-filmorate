package ru.yandex.practicum.filmorate.service;

import ru.yandex.practicum.filmorate.model.Director;

import java.util.Collection;

public interface DirectorService {
    Collection<Director> getAllDirectors();

    Director getDirectorById(Long id);

    Director updateDirector(Director director);

    Director createDirector(Director director);

    void deleteDirector(Long id);
}
