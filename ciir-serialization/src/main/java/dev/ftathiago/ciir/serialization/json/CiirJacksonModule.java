package dev.ftathiago.ciir.serialization.json;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import java.time.Instant;

/** Wires {@link CiirEnumSerializer} for every enum and {@link InstantSerializer} for timestamps. */
final class CiirJacksonModule extends SimpleModule {

  CiirJacksonModule() {
    addSerializer(Instant.class, new InstantSerializer());
    setSerializerModifier(
        new BeanSerializerModifier() {
          @Override
          public JsonSerializer<?> modifyEnumSerializer(
              SerializationConfig config,
              JavaType valueType,
              BeanDescription beanDesc,
              JsonSerializer<?> serializer) {
            return new CiirEnumSerializer();
          }
        });
  }
}
