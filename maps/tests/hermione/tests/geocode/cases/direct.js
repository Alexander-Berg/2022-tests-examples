describe('geocode/direct.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('geocode/cases/direct.html', {tileMock: "withParameters"})
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Прямое геокодирование. Данные должны соответствовать.', function () {
        return this.browser
            .pointerClick(PO.map.placemark.placemark())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .csCheckText('body #logger', 'Геокодирование \'Москва\'\nВсе данные геообъекта:\n[object Object]\nballoonContent:\n' +
                'Москва\nРоссия\n\nballoonContentBody:\nМосква\nРоссия\n\nboundedBy:\n55.142221,36.803259,56.021281,37.967682\n' +
                'description:\nРоссия\nname:\nМосква\ntext:\nРоссия, Москва\nuriMetaData.URI:\n' +
                'ymapsbm1://geo?ll=37.623%2C55.753&spn=1.164%2C0.879&text=%D0%A0%D0%BE%D1%81%D1%81%D0%B8%D1%8F%2C%20%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0\n' +
                'uriMetaData.URIs:\nymapsbm1://geo?ll=37.623%2C55.753&spn=1.164%2C0.879&text=%D0%A0%D0%BE%D1%81%D1%81%D0%B8%D1%8F%2C%20%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0\n' +
                'Метаданные ответа геокодера:\n[object Object]\nfound:\n1\nrequest:\nмосква\nresults:\n10\nskip:\n0\n' +
                'suggest:\nundefined\nМетаданные геокодера:\n[object Object]\nprecision\nother\nТип геообъекта: %s\n' +
                'province\nНазвание объекта: %s\nМосква\nОписание объекта: %s\nРоссия\nПолное описание объекта: %s\nРоссия, Москва');
    });
});
