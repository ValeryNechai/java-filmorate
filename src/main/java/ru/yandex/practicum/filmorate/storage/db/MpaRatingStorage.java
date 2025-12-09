package ru.yandex.practicum.filmorate.storage.db;

import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.Collection;

public interface MpaRatingStorage {
    MpaRating getMpaById(Integer id);

    Collection<MpaRating> getAllMpa();
}
