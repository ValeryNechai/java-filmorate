package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ReviewRatingsDbStorage implements ReviewRatingsStorage {
    private final JdbcTemplate jdbc;
    private final FeedStorage feedStorage;
    private static final String UPDATE_USEFUL_QUERY = "UPDATE REVIEWS SET USEFUL = USEFUL + ? WHERE REVIEW_ID = ?";
    private static final String GET_REACTION_QUERY =
            "SELECT IS_LIKE FROM REVIEW_RATINGS WHERE REVIEW_ID = ? AND USER_ID = ?";
    private static final String ADD_REACTION_TO_REVIEW_QUERY =
            "INSERT INTO REVIEW_RATINGS (REVIEW_ID, USER_ID, IS_LIKE) VALUES (?, ?, ?)";
    private static final String UPDATE_REACTION_TO_REVIEW_QUERY =
            "UPDATE REVIEW_RATINGS SET IS_LIKE = ? WHERE REVIEW_ID = ? AND USER_ID = ?";
    private static final String DELETE_REACTION_FROM_REVIEW_QUERY =
            "DELETE FROM REVIEW_RATINGS WHERE REVIEW_ID = ? AND USER_ID = ? AND IS_LIKE = ?";

    @Override
    public void addLikeToReview(Long reviewId, Long userId) {
        addReactionToReview(reviewId, userId, true);
        feedStorage.createFeed(userId, EventType.LIKE, Operation.ADD, reviewId);
        log.debug("Оценка отзыва успешно добавлена в базу данных.");
    }

    @Override
    public void addDislikeToReview(Long reviewId, Long userId) {
        addReactionToReview(reviewId, userId, false);
        feedStorage.createFeed(userId, EventType.LIKE, Operation.ADD, reviewId);
        log.debug("Оценка отзыва успешно добавлена в базу данных.");
    }

    @Override
    public void deleteLikeFromReview(Long reviewId, Long userId) {
        deleteReactionFromReview(reviewId, userId, true);
        feedStorage.createFeed(userId, EventType.LIKE, Operation.REMOVE, reviewId);
        log.debug("Оценка отзыва успешно удалена из базы данных.");
    }

    @Override
    public void deleteDislikeFromReview(Long reviewId, Long userId) {
        deleteReactionFromReview(reviewId, userId, false);
        feedStorage.createFeed(userId, EventType.LIKE, Operation.REMOVE, reviewId);
        log.debug("Оценка отзыва успешно удалена из базы данных.");
    }

    private void updateUseful(Long reviewId, int delta) {
        jdbc.update(UPDATE_USEFUL_QUERY, delta, reviewId);
    }

    private Boolean findReaction(Long reviewId, Long userId) {
        try {
            return jdbc.queryForObject(GET_REACTION_QUERY, Boolean.class, reviewId, userId);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Реакция к отзыву с id = " + reviewId + " от пользователя с id = " + userId + " не найдена!");
            return null;
        }
    }

    private void addReactionToReview(Long reviewId, Long userId, boolean isLike) {
        Boolean currentReaction = findReaction(reviewId, userId);
        int delta = 0;

        if (currentReaction == null) {
            jdbc.update(ADD_REACTION_TO_REVIEW_QUERY, reviewId, userId, isLike);
            delta = isLike ? 1 : -1;
            log.debug("Реакция успешно добавлена.");
        } else if (currentReaction == isLike) {
            log.debug("Реакция уже была добавлена ранее.");
        } else {
            jdbc.update(UPDATE_REACTION_TO_REVIEW_QUERY, isLike, reviewId, userId);
            delta = isLike ? 2 : -2;
            log.debug("Реакция изменена с {} на {}",
                    currentReaction ? "лайк" : "дизлайк",
                    isLike ? "лайк" : "дизлайк");
        }

        if (delta != 0) {
            updateUseful(reviewId, delta);
            log.debug("Рейтинг обновлен.");
        }
    }

    private void deleteReactionFromReview(Long reviewId, Long userId, boolean isLike) {
        Boolean currentReaction = findReaction(reviewId, userId);
        int delta = 0;

        if (currentReaction == null) {
            log.debug("Реакция не найдена");
        } else if (currentReaction == isLike) {
            jdbc.update(DELETE_REACTION_FROM_REVIEW_QUERY, reviewId, userId, isLike);
            delta = isLike ? -1 : 1;
            log.debug("Реакция успешно удалена.");
        } else {
            log.debug("На отзыве с id = {} стоит реакция {}, а не {}.",
                    reviewId, currentReaction ? "лайк" : "дизлайк", currentReaction ? "дизлайк" : "лайк");
        }

        if (delta != 0) {
            updateUseful(reviewId, delta);
            log.debug("Рейтинг обновлен.");
        }
    }
}
