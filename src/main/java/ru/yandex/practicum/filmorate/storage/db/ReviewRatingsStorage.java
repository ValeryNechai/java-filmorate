package ru.yandex.practicum.filmorate.storage.db;

public interface ReviewRatingsStorage {
    void addLikeToReview(Long reviewId, Long userId);

    void addDislikeToReview(Long reviewId, Long userId);

    void deleteLikeFromReview(Long reviewId, Long userId);

    void deleteDislikeFromReview(Long reviewId, Long userId);
}
