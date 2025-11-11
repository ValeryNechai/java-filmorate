# java-filmorate
Template repository for Filmorate project.

# Схема базы данных
![Database Schema SQL filmorate](src/main/resources/images/Database%20Schema%20SQL%20filmorate.png)

# Примеры запросов
### Получение всех фильмов
```sql
SELECT *
FROM films;
```

### Получение списка всех пользователей
```sql
SELECT *
FROM users;
```

### Получение топ 5 наиболее популярных фильмов
```sql
SELECT name
FROM films
WHERE film_id IN (SELECT film_id
                  FROM reaction 
                  GROUP BY film_id
                  ORDER BY COUNT(user_id) DESC
                  LIMIT 5);
```

### Получение списка общих друзей с другим пользователем
```sql
SELECT name
FROM users
WHERE user_id IN(SELECT f.user2_id AS friend_id
                 FROM friendship AS f
                 INNER JOIN status AS s ON f.status_id = s.status_id
                 WHERE f.user1_id = 123
                   AND s.name = 'Подтвержденная')
AND user_id IN(SELECT f.user2_id AS friend_id
               FROM friendship AS f
               INNER JOIN status AS s ON f.status_id = s.status_id
               WHERE f.user1_id = 124
                 AND s.name = 'Подтвержденная');
```