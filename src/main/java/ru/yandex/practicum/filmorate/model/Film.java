package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.deserializer.DurationMinutesDeserializer;
import ru.yandex.practicum.filmorate.serializer.DurationMinutesSerializer;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Film {
    private Long id;
    private String name;
    private String description;
    private LocalDate releaseDate;
    private Set<Long> likes;

    @JsonSerialize(using = DurationMinutesSerializer.class)
    @JsonDeserialize(using = DurationMinutesDeserializer.class)
    private Duration duration;

    public Set<Long> getLikes() {
        if (likes == null) {
            return likes = new HashSet<>();
        }
        return likes;
    }

    public void setLikes(Set<Long> likes) {
        this.likes = likes != null ? likes : new HashSet<>();
    }
}
