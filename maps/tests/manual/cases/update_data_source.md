<!--
Title:
Обновление данных карты

Description:
Проверка обновления данных отображаемых на карте
Памятка по терминам: https://wiki.yandex-team.ru/eva/testing/Projects/maps-api/

Components: 
smoke

Estimated time: 
180000

Precondition:
Перейти на тестовый стенд: https://front-jsapi.crowdtest.maps.yandex.ru/3.0/stand/index.html
При загрузке стенда появляется карта со спаном Москвы, слева имеется панель тестовых контролов, в центре карты зеленый маркер с координатами.

Step:
  Action:
  Выполнить клик в кнопку "vector". 

  Expectation:
  Карта в контейнере обновилась, маркер с координатами пропадает.

Step:
  Action:
  Выполнить клик в кнопку "Update data source". 

  Expectation:
  По клику в "Update data source", карта загружается в контейнере заново, на спане иконки и подписи организаций окрашиваются в красный цвет(все кроме транспортных).
  Под кнопкой "Update data source" появляются радио-баттоны "Scheme"(активен) и "Sattelite".

Step:
  Action:
  Скролом мыши призумить спан карты на несколько значений. 

  Expectation:
  Призум карты корректный, метки и подписи организаций окрашены красным цветом, отображение карты - растровое.

Step:
  Action:
  Выполнить клик в радио-баттон "Sattelite".

  Expectation:
  По клику подложка карты сменяется на спутниковый снимок, иконок, подписей на карте нет.

Step:
  Action:
  Скролом мыши отзумить спан карты на несколько значений. 

  Expectation:
  Отзум спана карты корректный, отображается спутниковый снимок карты.

Step:
  Action:
  Выполнить клик в радио-баттон "Scheme".

  Expectation:
  По клику подложка карты сменяется на схему, иконки и подписи организаций окрашены красным цветом.

Step:
  Action:
  Зажав ЛКМ дёрнули карту немного вправо-влево, вверх вниз(статичный и инертный драг) несколько раз.
  
  Expectation:
  Перемещение карты драгом корректное: нет странных визуальных эффектов на карте. 
  При драге метки и подписи не пропадают.
-->