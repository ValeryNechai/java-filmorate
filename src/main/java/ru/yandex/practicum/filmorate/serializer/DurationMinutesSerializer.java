package ru.yandex.practicum.filmorate.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Duration;

public class DurationMinutesSerializer extends JsonSerializer<Duration> {

    @Override
    public void serialize(Duration duration, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        if (duration == null) {
            jgen.writeNull();
        } else {
            jgen.writeNumber(duration.toMinutes());
        }
    }
}
