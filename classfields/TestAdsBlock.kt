package ru.auto.ara.devconfig

/**
 * @author airfreshener on 11.07.2018.
 */
object TestAdsBlock {
    /**
     * [https://wiki.yandex-team.ru/yandexmobile/ads/sdk/testing/demoadunits/]
     * */
    val TEST_NATIVE_ADS = listOf(
            "R-M-DEMO-native-c", //объявление типа  content со всеми ассетами
            "R-M-DEMO-native-i", //объявление типа  app install со всеми ассетами
            "R-M-DEMO-native-c-noage",  //объявление типа  content без  age
            "R-M-DEMO-native-c-nowarning",  //объявление типа  content без  warning
            "R-M-DEMO-native-c-wide",  //объявление типа  content с широкоформатным изображением
            "R-M-DEMO-native-c-large34",  //объявление типа  content с изображением формата 3:4
            "R-M-DEMO-native-c-large43",  //объявление типа  content с изображением формата 4:3
            "R-M-DEMO-native-c-large11",  //объявление типа  content с изображением формата 1:1
            "R-M-DEMO-native-i-noage",  //объявление типа  app install без  age
            "R-M-DEMO-native-i-nowarning",  //объявление типа  app install без  warning
            "R-M-DEMO-native-i-wide",  //объявление типа  app install с широкоформатным изображением
            "R-M-DEMO-native-i-large34",  //объявление типа  app install с изображением формата 3:4
            "R-M-DEMO-native-i-large43",  //объявление типа  app install с изображением формата 4:3
            "R-M-DEMO-native-i-large11"  //объявление типа  app install с изображением формата 1:1
    )

}