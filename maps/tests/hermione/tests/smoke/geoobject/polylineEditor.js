describe('smoke/geoobject/polylineEditor.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/polylineEditor.html')

            .waitReady()
            .waitForVisible(PO.map.pane.editor());
    });

    it.skip('Проверяем редактор линии', function () {
        return this.browser
            //Удаляем вершину через меню и снимаем скриншот
            .pointerClick(155, 153)
            .waitAndClick(PO.editorMenuItem() + '=Удалить точку')
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'after_load')

            //Выбираем продолжить через меню вершины и добавляем точку на карту
            .pointerClick(81, 152)
            .waitAndClick(PO.editorMenuItem() + '=Продолжить')
            .pause(200)
            .pointerClick(245, 77)

            //Удаляем точку даблкликом
            .moveToObject('body', 156, 281)
            .pointerDblClick(156, 281)
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'after_remove')

            //Удаляем линию через кастомный пункт меню и снимаем скриншот
            .pointerClick(245, 77)
            .waitAndClick(PO.editorMenuItem() + '=Удалить линию')
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'after_remove_line')
            .verifyNoErrors();
    });
});
