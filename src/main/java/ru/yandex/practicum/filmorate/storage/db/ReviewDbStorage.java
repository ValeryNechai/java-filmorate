package ru.yandex.practicum.filmorate.storage.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class ReviewDbStorage extends AbstractDbStorage<Review> implements ReviewStorage {
    private static final String INSERT_REVIEW_QUERY =
            "INSERT INTO REVIEWS (CONTENT, IS_POSITIVE, USER_ID, FILM_ID) " +
                    "VALUES (?, ?, ?, ?)";
    private static final String UPDATE_REVIEW_QUERY = "UPDATE REVIEWS SET CONTENT = ?, IS_POSITIVE = ? " +
            "WHERE REVIEW_ID = ?";
    private static final String DELETE_REVIEW_QUERY = "DELETE FROM REVIEWS WHERE REVIEW_ID = ?";
    private static final String FIND_REVIEW_BY_ID_QUERY = "SELECT * FROM REVIEWS WHERE REVIEW_ID = ?";
    private static final String FIND_REVIEW_BY_FILM_ID_AND_COUNT =
            "SELECT * FROM REVIEWS WHERE FILM_ID = ? ORDER BY USEFUL DESC LIMIT ?";
    private static final String FIND_REVIEW_BY_FILM_ID = "SELECT REVIEW_ID FROM REVIEWS WHERE FILM_ID = ?";
    private static final String FIND_REVIEW_BY_ALL_FILMS_AND_COUNT =
            "SELECT * FROM REVIEWS ORDER BY USEFUL DESC LIMIT ?";
    private static final String FIND_REVIEWS_ID_BY_ALL_FILMS_QUERY = "SELECT REVIEW_ID, FILM_ID FROM REVIEWS";

    public ReviewDbStorage(JdbcTemplate jdbc, RowMapper<Review> mapper) {
        super(jdbc, mapper);
    }

    @Override
    public Review createReview(Review review) {
        long id = insert(
                INSERT_REVIEW_QUERY,
                review.getContent(),
                review.getIsPositive(),
                review.getUserId(),
                review.getFilmId()
        );
        review.setReviewId(id);
        log.debug("Отзыв успешно добавлен в базу данных.");

        return getReviewById(review.getReviewId());
    }

    @Override
    public Review updateReview(Review newReview) {
        update(
                UPDATE_REVIEW_QUERY,
                newReview.getContent(),
                newReview.getIsPositive(),
                newReview.getReviewId()
        );

        log.debug("Отзыв успешно обновлен в базе данных.");

        return getReviewById(newReview.getReviewId());
    }

    @Override
    public void deleteReview(Long reviewId) {
        update(
                DELETE_REVIEW_QUERY,
                reviewId
        );
        log.debug("Отзыв успешно удален из базы данных.");
    }

    @Override
    public Review getReviewById(Long reviewId) {
        return findOne(FIND_REVIEW_BY_ID_QUERY, reviewId)
                .orElseThrow(() -> new NotFoundException("Отзыв с id = " + reviewId + " не найден"));
    }

    @Override
    public Set<Long> getReviewsByFilmId(Long filmId) {
        List<Long> reviewIds = jdbc.queryForList(FIND_REVIEW_BY_FILM_ID, Long.class, filmId);
        return new HashSet<>(reviewIds);
    }

    @Override
    public Collection<Review> getReviewsByFilmIdAndCount(Long filmId, int count) {
        return findMany(FIND_REVIEW_BY_FILM_ID_AND_COUNT, filmId, count);
    }

    @Override
    public Collection<Review> getReviewsByAllFilmsAndCount(int count) {
        return findMany(FIND_REVIEW_BY_ALL_FILMS_AND_COUNT, count);
    }

    @Override
    public Map<Long, Set<Long>> getReviewsByAllFilms() {
        return jdbc.query(FIND_REVIEWS_ID_BY_ALL_FILMS_QUERY, new ResultSetExtractor<Map<Long, Set<Long>>>() {
            @Override
            public Map<Long, Set<Long>> extractData(ResultSet rs) throws SQLException, DataAccessException {
                Map<Long, Set<Long>> result = new HashMap<>();

                while (rs.next()) {
                    Long reviewId = rs.getLong("REVIEW_ID");
                    Long filmId = rs.getLong("FILM_ID");
                    result.computeIfAbsent(filmId, k -> new HashSet<>()).add(reviewId);
                }
                return result;
            }
        });
    }

    @Override
    public Map<Long, Set<Long>> getReviewsByFilmIds(Set<Long> filmIds) {
        String placeholders = filmIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));
        String findReviewsByFilmIdsQuery = String.format("SELECT FILM_ID, REVIEW_ID FROM REVIEWS " +
                "WHERE FILM_ID IN (%s)", placeholders);

        return jdbc.query(findReviewsByFilmIdsQuery, filmIds.toArray(), new ResultSetExtractor<Map<Long, Set<Long>>>() {
            @Override
            public Map<Long, Set<Long>> extractData(ResultSet rs) throws SQLException, DataAccessException {
                Map<Long, Set<Long>> result = new HashMap<>();

                while (rs.next()) {
                    Long reviewId = rs.getLong("REVIEW_ID");
                    Long filmId = rs.getLong("FILM_ID");
                    result.computeIfAbsent(filmId, k -> new HashSet<>()).add(reviewId);
                }
                return result;
            }
        });
    }

    @Override
    public boolean existsById(Long reviewId) {
        String existsByIdQuery = "SELECT COUNT(*) FROM REVIEWS WHERE REVIEW_ID = ?";
        if (reviewId == null) {
            return false;
        }

        Integer count = jdbc.queryForObject(
                existsByIdQuery,
                Integer.class,
                reviewId
        );

        return count != null && count > 0;
    }
}
