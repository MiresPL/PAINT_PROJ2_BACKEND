package com.mires.paint.common;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/* *
 * Created by Joshua Bell (RingOfStorms)
 *
 * Post explaining here: [URL]http://bukkit.org/threads/gsonfactory-gson-that-works-on-itemstack-potioneffect-location-objects.331161/[/URL]
 * */
public class GsonFactory {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Ignore {
    }

    /*
    - I want to not use Bukkit parsing for most objects... it's kind of clunky
    - Instead... I want to start using any of Mojang's tags
    - They're really well documented + built into MC, and handled by them.
    - Rather than kill your old code, I'm going to write TypeAdapaters using Mojang's stuff.
     */

    private static final Gson g = new Gson();

    private final static String CLASS_KEY = "SERIAL-ADAPTER-CLASS-KEY";

    private static Gson prettyGson;
    private static Gson compactGson;

    /**
     * Returns a Gson instance for use anywhere with new line pretty printing
     * <p>
     * Use @GsonIgnore in order to skip serialization and deserialization
     * </p>
     *
     * @return a Gson instance
     */
    public static Gson getPrettyGson() {
        if (prettyGson == null)
            prettyGson = new GsonBuilder().addSerializationExclusionStrategy(new ExposeExlusion())
                    .addDeserializationExclusionStrategy(new ExposeExlusion())
                    .registerTypeAdapter(Date.class, new DateGsonAdapter())
                    .registerTypeAdapter(Long.class, new NumberLongAdapter())
                    .registerTypeAdapter(long.class, new NumberLongAdapter())
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();
        return prettyGson;
    }

    /**
     * Returns a Gson instance for use anywhere with one line strings
     * <p>
     * Use @GsonIgnore in order to skip serialization and deserialization
     * </p>
     *
     * @return a Gson instance
     */
    public static Gson getCompactGson() {
        if (compactGson == null)
            compactGson = new GsonBuilder()
                    .addSerializationExclusionStrategy(new ExposeExlusion())
                    .addDeserializationExclusionStrategy(new ExposeExlusion())
                    .registerTypeAdapter(Date.class, new DateGsonAdapter())
                    .registerTypeAdapter(Long.class, new NumberLongAdapter())
                    .registerTypeAdapter(long.class, new NumberLongAdapter())
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();
        return compactGson;
    }

    /**
     * Creates a new instance of Gson for use anywhere
     * <p>
     * Use @GsonIgnore in order to skip serialization and deserialization
     * </p>
     *
     * @return a Gson instance
     */


    private static Map<String, Object> recursiveDoubleToInteger(Map<String, Object> originalMap) {
        Map<String, Object> map = new HashMap<>();
        for (Entry<String, Object> entry : originalMap.entrySet()) {
            Object o = entry.getValue();
            if (o instanceof Double) {
                Double d = (Double) o;
                Integer i = d.intValue();
                map.put(entry.getKey(), i);
            } else if (o instanceof Map) {
                Map<String, Object> subMap = (Map<String, Object>) o;
                map.put(entry.getKey(), recursiveDoubleToInteger(subMap));
            } else {
                map.put(entry.getKey(), o);
            }
        }
        return map;
    }

    private static class ExposeExlusion implements ExclusionStrategy {
        @Override
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            final Ignore ignore = fieldAttributes.getAnnotation(Ignore.class);
            if (ignore != null)
                return true;
            final Expose expose = fieldAttributes.getAnnotation(Expose.class);
            return expose != null && (!expose.serialize() || !expose.deserialize());
        }

        @Override
        public boolean shouldSkipClass(Class<?> aClass) {
            return false;
        }
    }



    private static class DateGsonAdapter extends TypeAdapter<Date> {
        @Override
        public void write(JsonWriter jsonWriter, Date date) throws IOException {
            if (date == null) {
                jsonWriter.nullValue();
                return;
            }
            jsonWriter.value(date.getTime());
        }

        @Override
        public Date read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            return new Date(jsonReader.nextLong());
        }
    }

    private static class LongWrapper {
        @SerializedName("$numberLong")
        private String numberLong;

        public long getNumberLong() {
            return Long.parseLong(numberLong);
        }
    }

    private static class NumberLongAdapter extends TypeAdapter<Number> {
        @Override
        public void write(JsonWriter jsonWriter, Number value) throws IOException {
            if (value == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(value.longValue());
            }
        }

        @Override
        public Number read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }

            if (jsonReader.peek() == JsonToken.BEGIN_OBJECT) {
                LongWrapper longWrapper = new Gson().fromJson(jsonReader, LongWrapper.class);
                return longWrapper.getNumberLong();
            } else if (jsonReader.peek() == JsonToken.NUMBER) {
                return jsonReader.nextLong();
            } else if (jsonReader.peek() == JsonToken.STRING) {
                String stringValue = jsonReader.nextString();
                try {
                    return Long.parseLong(stringValue);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return 0L;
                }
            } else {
                return 0L;
            }
        }
    }
}
