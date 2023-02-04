describe('geocode/correction.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('geocode/cases/correction.html', {tileMock: "withParameters"})
            .waitReady();
    });
    it('Запрос мfсква должен исправиться', function () {
        return this.browser
            .pause(500)
            .csCheckText('body #logger', 'Геокодирование \'мfсква\'\nМетаданные ответа геокодера, correction:\nм,о,сква')
            .csVerifyMapScreenshot(PO.mapId(), 'map');
    });
});
