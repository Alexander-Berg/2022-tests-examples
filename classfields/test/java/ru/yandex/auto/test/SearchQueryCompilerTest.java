package ru.yandex.auto.test;

/** User: yan1984 Date: 23.05.2011 17:07:00 */
// TODO remove this file completely
public class SearchQueryCompilerTest extends AbstractSearchConfigurationBasedTests {
  /*
  @Test
  public void testCompileQueryAndFilters() {
      long time1 = System.currentTimeMillis();

      Query query = new SearchQueryCompiler().compileQuery(getSearchConfiguration(), SearchQueryFilters.CAR_AD_FILTERS, null);
      System.out.println(TimerUtils.pastMillis(time1));

      Assert.assertEquals(
              "filtered(filtered(filtered(filtered(filtered((+mark_code:FORD +model_code:FOCUS) (+mark_code:FORD +model_code:MONDEO))->year:[1990 TO *})->price_rur:[10000 TO 100000])->run:[0 TO 100000])->creation_date:[60 TO *})->custom_house_state:CLEARED_BY_CUSTOMS",
              query.toString()
      );
  }
  */
}
