package ru.yandex.practicum.filmorate.storage.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;

import java.util.Collection;

@Repository
public class DirectorDbStorage extends AbstractDbStorage<Director>
        implements DirectorStorage {

    private static final String FIND_ALL_QUERY =
            "SELECT DIRECTOR_ID, DIRECTOR_NAME FROM DIRECTORS ORDER BY DIRECTOR_ID";

    private static final String FIND_BY_ID_QUERY =
            "SELECT DIRECTOR_ID, DIRECTOR_NAME FROM DIRECTORS WHERE DIRECTOR_ID = ?";

    private static final String INSERT_QUERY =
            "INSERT INTO DIRECTORS (DIRECTOR_NAME) VALUES (?)";

    private static final String UPDATE_QUERY =
            "UPDATE DIRECTORS SET DIRECTOR_NAME = ? WHERE DIRECTOR_ID = ?";

    private static final String DELETE_QUERY =
            "DELETE FROM DIRECTORS WHERE DIRECTOR_ID = ?";

    public DirectorDbStorage(JdbcTemplate jdbc, RowMapper<Director> mapper) {
        super(jdbc, mapper);
    }

    @Override
    public Collection<Director> getAllDirectors() {
        return findMany(FIND_ALL_QUERY);
    }

    @Override
    public Director getDirectorById(Long id) {
        return findOne(FIND_BY_ID_QUERY, id)
                .orElseThrow(() ->
                        new NotFoundException("Режиссёр с id = " + id + " не найден")
                );
    }

    @Override
    public Director createDirector(Director director) {
        long id = insert(INSERT_QUERY, director.getName());
        director.setId(id);
        return director;
    }

    @Override
    public Director updateDirector(Director director) {
        update(
                UPDATE_QUERY,
                director.getName(),
                director.getId()
        );
        return getDirectorById(director.getId());
    }

    @Override
    public Director deleteDirector(Long id) {
        Director director = getDirectorById(id);
        update(DELETE_QUERY, id);
        return director;
    }
}


