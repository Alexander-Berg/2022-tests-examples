package ru.yandex.auto.clone.unifier.parser;

import static org.junit.Assert.*;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.auto.core.model.UnifiedCarInfo;

/** @author obolshakova */
public class DescriptionParserTest {
  private static final DescriptionParser PARSER = new DescriptionParser();

  @Nullable private UnifiedCarInfo info = null;

  @Before
  public void setUp() {
    info = new UnifiedCarInfo("1");
  }

  @Test
  public void test1() {
    final String description =
        "60000 км. после кап. ремонта двигателя. Новый карбюратор, стартер, аккумулятор, замена тормозной системы (все новое). "
            + "Все делалось своими руками под себя. На хорошем ходу, не гнилой. тел. 8926 588-92-05 Денис";
    info.setDescription(description);
    PARSER.modify(info);

    assertNull(info.getPrice());
    assertNull(info.getCurrencyType());
    assertNotNull(info.getSellerPhone());
    assertNull(info.getYear());
    assertNotNull(info.getMileage());
    assertNotNull(info.getMileageMetric());
    assertEquals(info.getMileageMetric(), "км");
    assertEquals(info.getMileage(), new Integer(60000));
  }

  @Test
  public void test2() {
    final String description =
        "Автомобиль прибыл в Москву 07.2007 г. из Мюнхина, Германия. Покупался у Официального дилера. Есть все док-ты. "
            + "Состояние отличное. Все работает. Пройдено полное ТО, в Германии. Комплектация \"FAHION\" климатконтроль, люк, столики, зонтик и т.д.";
    info.setDescription(description);
    PARSER.modify(info);

    assertNull(info.getPrice());
    assertNull(info.getCurrencyType());
    assertNull(info.getSellerPhone());
    assertNotNull(info.getYear());
    assertNull(info.getMileage());
    assertNull(info.getMileageMetric());
  }

  @Test
  public void test3() {
    final String description =
        "Автомобиль в хорошем состоянии. Пригнана из Германии в мае 2005 году, один хозяин в Москве. "
            + "Обслуживалась у официального дилера BMW.";
    info.setDescription(description);
    PARSER.modify(info);

    assertNull(info.getPrice());
    assertNull(info.getCurrencyType());
    assertNull(info.getSellerPhone());
    assertNotNull(info.getYear());
    assertNull(info.getMileage());
    assertNull(info.getMileageMetric());
  }

  @Test
  public void test4() {
    final String description =
        "Продается Хенде Гетц GLS, 1.3 МКПП-5,  (Hyundai Getz) куплен в декабре 2003 г.в., начало эксплуатации с 2004. "
            + "Светло-голубой металлик, ГУР, подушка безопасности водителя, пробег 49000км, сервисная книжка, все ТО по регламенту. "
            + "Последнее ТО на 45000км&#61514;. Кондиционер, электропривод зеркал, стеклоподъемники, подогрев сидений, коврики, "
            + "кассетная магнитола(штатная). Машина в отличном состоянии не требующая вложений, не битая. По гарантии перекрашивался капот и дверь.";
    info.setDescription(description);
    PARSER.modify(info);

    assertNull(info.getPrice());
    assertNull(info.getCurrencyType());
    assertNull(info.getSellerPhone());
    assertNotNull(info.getYear());
    assertNotNull(info.getMileage());
    assertNotNull(info.getMileageMetric());
  }

  @Test
  public void test5() {
    final String description =
        "переборка двигателя  15т.км,экономичен расход по городу 7-8литров саляры!!!! есть проблемы по кузову на фото видно";
    info.setDescription(description);
    PARSER.modify(info);

    assertNull(info.getPrice());
    assertNull(info.getCurrencyType());
    assertNull(info.getSellerPhone());
    assertNull(info.getYear());
    assertNotNull(info.getMileage());
    assertNotNull(info.getMileageMetric());
  }

  @Test
  public void test6() {
    final String description =
        "ВАЗ-21093i, 2003 г.в., 78000 км, синий мет., 1500, гаражн., не битая, ухожена, музыка, сигн., отл.сост., "
            + "130 тыс.р. 8-921-997-32-41<br/>Дата выхода объявления в газете: 03.09.2007";
    info.setDescription(description);
    PARSER.modify(info);

    assertNotNull(info.getPrice());
    assertNotNull(info.getCurrencyType());
    assertNotNull(info.getSellerPhone());
    assertNotNull(info.getYear());
    assertNotNull(info.getMileage());
    assertNotNull(info.getMileageMetric());
    assertEquals(info.getMileage(), new Integer(78000));
  }

  @Test
  public void test7() {
    final String description =
        "Ауди-80 1987 г/в, цв. белый, двиг. 1.8, карбюратор, фаркоп, музыка, литые диски, тверской учет, продаю за 90 тыс. руб. 8-903-630-69-32.";
    info.setDescription(description);
    PARSER.modify(info);

    assertNotNull(info.getPrice());
    assertNotNull(info.getCurrencyType());
    assertNotNull(info.getSellerPhone());
    assertNotNull(info.getYear());
    assertNull(info.getMileage());
    assertNull(info.getMileageMetric());
  }
}
