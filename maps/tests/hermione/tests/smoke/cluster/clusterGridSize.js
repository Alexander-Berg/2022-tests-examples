describe('smoke/cluster/clusterGridSize.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/cluster/clusterGridSize.html')

            //Дожидаемся карты
            .waitReady();
    });

    it('Добавляем 100 объектов с gridSize = 64 и снимаем скриншот', function () {
        return this.browser
            // Добавляем 100 объектов с gridSize = 64 и снимаем скриншот
            .waitAndClick('body #useClusterer')
            .waitAndClick('body #addMarkers')
            .waitForVisible(PO.map.placemark())
            .pause(300)
            .csVerifyMapScreenshot(PO.mapId(), 'gridSize64')
            .verifyNoErrors();
    });

    it('Добавляем 100 объектов с gridSize = 10 и снимаем скриншот', function () {
        return this.browser
            // Добавляем 100 объектов с gridSize = 10 и снимаем скриншот
            .waitAndClick('body #useClusterer')
            .setValue('body #gridSize', '10')
            .waitAndClick('body #addMarkers')
            .waitForVisible(PO.map.placemark())
            .pause(300)
            .csVerifyMapScreenshot(PO.mapId(), 'gridSize10')
            .verifyNoErrors();
    });

    it('Добавляем 100 объектов с gridSize = 1000 и снимаем скриншот', function () {
        return this.browser
            // Добавляем 100 объектов с gridSize = 1000 и снимаем скриншот
            .waitAndClick('body #useClusterer')
            .setValue('body #gridSize', '1000')
            .waitAndClick('body #addMarkers')
            .waitForVisible(PO.map.placemark())
            .pause(300)
            .csVerifyMapScreenshot(PO.mapId(), 'gridSize1000')
            .verifyNoErrors();
    });

    it('Добавляем 100 объектов с gridSize = 500 и сравниваем скриншот с gridSize = 1000', function () {
        return this.browser
            // Добавляем 100 объектов с gridSize = 500 и сравниваем скриншот с gridSize = 1000
            .waitAndClick('body #useClusterer')
            .setValue('body #gridSize', '500')
            .waitAndClick('body #addMarkers')
            .waitForVisible(PO.map.placemark())
            .pause(300)
            .csVerifyMapScreenshot(PO.mapId(), 'gridSize1000')
            .verifyNoErrors();
    });
});
