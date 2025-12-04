package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Integer duration;

    @JsonProperty("mpa")
    private MpaRating mpaRating;
    private Set<Long> likes = new HashSet<>();

    @JsonProperty("genres")
    private Set<Genre> filmGenres = new HashSet<>();
}
