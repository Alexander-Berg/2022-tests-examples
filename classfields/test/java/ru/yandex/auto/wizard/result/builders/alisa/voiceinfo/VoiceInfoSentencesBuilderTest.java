package ru.yandex.auto.wizard.result.builders.alisa.voiceinfo;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.auto.core.wizard.alisa.VoiceInfoSentencesBuilder;
import ru.yandex.auto.wizard.utils.TextUtils;

public class VoiceInfoSentencesBuilderTest extends Assert {

  private final VoiceInfoSentencesBuilder voiceInfoSentencesBuilder =
      new VoiceInfoSentencesBuilder();

  @Test
  public void buildMarkModelSentence() {
    voiceInfoSentencesBuilder
        .buildMarkModelSentence("БМВ", "3 серия")
        .ifPresent(s -> assertEquals("Это БМВ 3 серия", s));
  }

  @Test
  public void buildGenerationSentence() {
    voiceInfoSentencesBuilder
        .buildGenerationSentence("1975", null, false, false)
        .ifPresent(s -> assertEquals("Модель выпускается c 1975 года по настоящее время", s));
  }

  @Test
  public void buildClassAndBodytypesSentence() {
    voiceInfoSentencesBuilder
        .buildClassAndBodytypesSentence(null, Arrays.asList("седан", "хэтчбек", "универсал"), false)
        .ifPresent(
            s -> assertEquals("Автомобиль представлен в кузове седан, хэтчбек или универсал", s));
  }

  @Test
  public void buildTransmissionSentence() {
    voiceInfoSentencesBuilder
        .buildTransmissionSentence(Arrays.asList("механика", "автомат"))
        .ifPresent(
            s -> assertEquals("Коробка передач" + TextUtils.Dash() + "механика или автомат", s));
  }

  @Test
  public void buildEngineTypeSentence() {
    voiceInfoSentencesBuilder
        .buildEngineTypeSentence(Arrays.asList("бензиновым", "дизельным", "гибридным"), 116, 326)
        .ifPresent(
            s ->
                assertEquals(
                    "Оснащается бензиновым, дизельным или гибридным двигателем мощностью от 116 до 326 лошадиных сил",
                    s));
  }

  @Test
  public void buildGearTypesSentence() {
    voiceInfoSentencesBuilder
        .buildGearTypesSentence(Arrays.asList("задний", "полный"))
        .ifPresent(s -> assertEquals("Задний или полный привод", s));
  }
}
