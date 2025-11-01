package ru.yandex.practicum.filmorate.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Duration;

public class DurationMinutesDeserializer extends JsonDeserializer<Duration> {

    @Override
    public Duration deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        Long minutes = jsonParser.getLongValue();
        return Duration.ofMinutes(minutes);
    }
}
