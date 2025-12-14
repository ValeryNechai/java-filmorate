package ru.yandex.practicum.filmorate.storage.db;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface ReviewStorage {
    Review createReview(Review review);

    Review updateReview(Review newReview);

    void deleteReview(Long reviewId);

    Review getReviewById(Long reviewId);

    boolean existsById(Long id);

    Collection<Review> getReviewsByFilmIdAndCount(Long filmId, int count);

    Collection<Review> getReviewsByAllFilmsAndCount(int count);

    Map<Long, Set<Long>> getReviewsByFilmIds(Set<Long> filmIds);

    Map<Long, Set<Long>> getReviewsByAllFilms();
}
