package ru.yandex.practicum.filmorate.service.db;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.service.DirectorService;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class DirectorDbService implements DirectorService {

    private final DirectorStorage directorStorage;

    @Override
    public Collection<Director> getAllDirectors() {
        return directorStorage.getAllDirectors();
    }

    @Override
    public Director getDirectorById(Long id) {
        if (id == null) {
            throw new ValidationException("ID режиссёра не может быть null");
        }
        return directorStorage.getDirectorById(id);
    }

    @Override
    public Director createDirector(Director director) {
        validateDirector(director);
        return directorStorage.createDirector(director);
    }

    @Override
    public Director updateDirector(Director director) {
        validateDirector(director);

        if (director.getId() == null) {
            throw new ValidationException("ID режиссёра обязателен для обновления");
        }

        directorStorage.getDirectorById(director.getId());

        return directorStorage.updateDirector(director);
    }

    @Override
    public void deleteDirector(Long id) {
        if (id == null) {
            throw new ValidationException("ID режиссёра не может быть null");
        }

        directorStorage.deleteDirector(id);
    }

    private void validateDirector(Director director) {
        if (director == null) {
            throw new ValidationException("Режиссёр не может быть null");
        }
        if (director.getName() == null || director.getName().isBlank()) {
            throw new ValidationException("Имя режиссёра не может быть пустым");
        }
    }
}


