package ro.tuc.ds.user.config;

import tools.jackson.databind.json.JsonMapper;

/** Single place that touches the JSON library (Jackson 3, the default in Spring Boot 4). */
public final class Json {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private Json() {
    }

    public static <T> T read(String json, Class<T> type) {
        return MAPPER.readValue(json, type);
    }

    public static String write(Object value) {
        return MAPPER.writeValueAsString(value);
    }
}
