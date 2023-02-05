ymaps.modules.define(util.testfile(), [
    'Map',
    'templateLayoutFactory',
    'Balloon'
], function (provide, Map, templateLayoutFactory, Balloon) {

    describe('Balloon', function () {
        // Добавляем время на загрузку оверлея и макета балуна
        this.timeout(10000);
        var geoMap, balloon;
        beforeEach(function () {
            geoMap = new Map('map', {
                center: [50.74954, 31.621587],
                zoom: 10,
                behaviors: [],
                controls: [],
                type: null
            });
        });

        afterEach(function () {
            geoMap.destroy();
            geoMap = null;
        });

        describe('Методы', function () {

            // TODO перенести тесты из Balloon.test.html

            describe('autoPan', function () {

                beforeEach(function () {
                    balloon = new Balloon(geoMap);
                    balloon.options.setParent(geoMap.options);
                });

                afterEach(function () {
                    balloon.destroy();
                    balloon = null;
                });

                // TODO разбить тест testAutoPan из Balloon.test.html на мелкие тесты.

                it('Должен применить автопозиционирование со стандартным значением autoPanMargin', function (done) {
                    balloon.events.once('autopanend', function () {
                        var projection = geoMap.options.get('projection'),
                            zoom = geoMap.getZoom(),
                            layoutSize = balloon.getOverlaySync().getLayoutSync().getShape().getBounds();
                        // Проверяем, что центр карты смещен на половину ширины балуна + отступ.
                        expect(projection.toGlobalPixels(geoMap.getBounds()[0], zoom)[0] - projection.toGlobalPixels(balloon.getPosition(), zoom)[0]).to.be(layoutSize[0][0] - 34);
                        done();
                    });
                    balloon.open([40, 30]).fail(function (error) {
                        done(error);
                    });
                });

                it('Должен применить автопозиционирование с учетом опции autoPanMargin', function (done) {
                    // Используем только margin карты.
                    balloon.options.set('autoPanMargin', 10);

                    balloon.events.once('autopanend', function () {
                        var projection = geoMap.options.get('projection'),
                            zoom = geoMap.getZoom(),
                            layoutSize = balloon.getOverlaySync().getLayoutSync().getShape().getBounds();
                        // Проверяем, что центр карты смещен на половину ширины балуна + отступ.
                        expect(projection.toGlobalPixels(geoMap.getBounds()[0], zoom)[0] - projection.toGlobalPixels(balloon.getPosition(), zoom)[0]).to.be(layoutSize[0][0] - 10);
                        done();
                    });
                    balloon.open([40, 30]).fail(function (error) {
                        done(error);
                    });
                });


                it('Должен применить автопозиционирование после открытия балуна с учетом отступов карты', function (done) {
                    // Используем только margin карты.
                    balloon.options.set('autoPanMargin', 0);

                    geoMap.margin.addArea({
                        left: 0,
                        top: 0,
                        width: 200,
                        height: '100%'
                    });

                    balloon.events.once('autopanend', function () {
                        var projection = geoMap.options.get('projection'),
                            zoom = geoMap.getZoom(),
                            layoutSize = balloon.getOverlaySync().getLayoutSync().getShape().getBounds();
                        // Проверяем, что центр карты смещен на половину ширины балуна + отступ.
                        expect(projection.toGlobalPixels(geoMap.getBounds()[0], zoom)[0] - projection.toGlobalPixels(balloon.getPosition(), zoom)[0]).to.be(layoutSize[0][0] - 200);
                        done();
                    });
                    balloon.open([40, 30]).fail(function (error) {
                        done(error);
                    });
                });

                it('Должен применить автопозиционирование после открытия балуна без учета всех отступов', function (done) {
                    // Используем только margin карты.
                    balloon.options.set({
                        autoPanMargin: 0,
                        autoPanUseMapMargin: false
                    });

                    geoMap.margin.addArea({
                        left: 0,
                        top: 0,
                        width: 200,
                        height: '100%'
                    });

                    balloon.events.once('autopanend', function () {
                        var projection = geoMap.options.get('projection'),
                            zoom = geoMap.getZoom(),
                            layoutSize = balloon.getOverlaySync().getLayoutSync().getShape().getBounds();
                        // Проверяем, что центр карты смещен на половину ширины балуна.
                        expect(projection.toGlobalPixels(geoMap.getBounds()[0], zoom)[0] - projection.toGlobalPixels(balloon.getPosition(), zoom)[0]).to.be(layoutSize[0][0]);
                        done();
                    });

                    balloon.open([40, 30]).fail(function (error) {
                        done(error);
                    });
                });
            });
        });

    });

    provide(true);
});
