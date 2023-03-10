<!--
Title:
Базовая проверка ограничения зума

Description:
Проверка ограничения зума.
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
  Выполнить клик в кнопку "Map zoom range 5-10".

  Expectation:
  По клику в "Map zoom range 5-10", карта загружается в контейнере заново на более дальнем зуме. Контрол зума "+" неактивен(окрашен серым).

Step:
  Action:
  Выполнить даблклик в карту.

  Expectation:
  При даблклике происходит небольшое смещение спана карты, карта не призумливается.

Step:
  Action:
  Зажав левую кнопку мышки оттащить карту в сторону. 

  Expectation:
  Драг карты корректный, нет странных визуальных эффектов на карте.
  Карта не дрожит и не мигает цветами, отрисовка элементов и подписей корректная.

Step:
  Action:
  Выполнить 5 даблкликов ПКМ в карту.

  Expectation:
  При даблклике ПКМ происходит корректный отзум спана карты. После 5 даблклика контрол зума "- неактивен(окрашен серым).

Step:
  Action:
  Выполнить даблклик ПКМ в карту.

  Expectation:
  При даблклике ПКМ происходит небольшое смещение спана карты, отзум спана карты не срабатывает.

Step:
  Action:
  Максимально призумить/отзумить карты скролом мыши.

  Expectation:
  Скролзум работает корректно, но призум/отзум на карте ограничен.
-->