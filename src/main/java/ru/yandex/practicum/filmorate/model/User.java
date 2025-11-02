package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
public class User {
    private Long id;
    private String email;
    private String login;
    private String name;
    private LocalDate birthday;
    private Set<Long> friends;

    public Set<Long> getFriends() {
        if (friends == null) {
            return friends = new HashSet<>();
        }
        return friends;
    }

    public void setFriends(Set<Long> friends) {
        this.friends = friends != null ? friends : new HashSet<>();
    }
}
