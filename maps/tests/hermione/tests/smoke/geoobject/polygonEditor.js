describe('smoke/geoobject/polygonEditor.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/polygonEditor.html')

            .waitReady()
            .waitForVisible(PO.map.controls.zoom.plus());
    });

    it('Проверяем рисование с align:center', function () {
        return this.browser
            //Рисуем две точки и проверяем наводящие линии скриншотом
            .pause(200)
            .pointerClick(175, 284)
            .pointerClick(159, 412)
            .moveToObject('body', 283, 420)
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), '2_points')

            //Дорисовываем треугольник
            .pointerClick(283, 420)

            //Открываем меню у точки
            .pointerClick(159, 412)

            //Включаем добавление нового контура
            .waitAndClick(PO.editorMenuItem() + '=Добавить внутренний контур')

            //Добавляем новый контур и делаем скриншот
            .pointerClick(203, 377)
            .pointerClick(283, 266)
            .pointerClick(345, 365)
            .moveToObject('body', 365, 385)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), '6_points')

            //Открываем меню и удаляем одну точку
            .pointerClick(345, 365)
            .waitAndClick(PO.editorMenuItem() + '=Удалить точку')

            //Открываем меню и кликаем продолжить
            .pointerClick(283, 266)
            .waitAndClick(PO.editorMenuItem() + '=Продолжить')

            //Дорисовываем до 6 точек и делаем скриншот
            .pointerClick(204, 465)
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), '6_another_points')

            //Удаляем контур через меню и проверяем что удалился
            .pointerClick(204, 465)
            .waitAndClick(PO.editorMenuItem() + '=Удалить контур')
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), '3_points')
            .verifyNoErrors();
    });
});
