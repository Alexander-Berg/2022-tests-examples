package ru.yandex.auto.searcher.converter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import ru.yandex.auto.core.search2.JacksonXmlBuilder;
import ru.yandex.auto.searcher.result.data.unifyMarkModel.ReviewUnificationEntry;
import ru.yandex.auto.searcher.result.data.unifyMarkModel.UnifyMarkAndModelResult;

/** User: daedra Date: 13.04.13 Time: 4:45 */
public class JacksonXmlBuilderTest {
  @Test
  public void testMarkAndModel() throws Exception {
    JacksonXmlBuilder builder = new JacksonXmlBuilder();
    builder.afterPropertiesSet();

    List<ReviewUnificationEntry> rewiewUnificationEntries =
        new LinkedList<ReviewUnificationEntry>();
    ReviewUnificationEntry mm = new ReviewUnificationEntry();
    mm.setRawMark("ford");
    mm.setRawModel("focus");
    mm.setMarkCode("FORD");
    rewiewUnificationEntries.add(mm);

    mm = new ReviewUnificationEntry();
    mm.setRawMark("mazda");
    mm.setRawModel("3");
    rewiewUnificationEntries.add(mm);
    UnifyMarkAndModelResult unifyMarkAndModelResult =
        new UnifyMarkAndModelResult(rewiewUnificationEntries);

    String result = builder.build(Collections.singletonList(unifyMarkAndModelResult), null);
    System.out.println("result = " + result);
  }
}
