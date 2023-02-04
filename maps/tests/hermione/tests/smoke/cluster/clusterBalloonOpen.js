describe('smoke/cluster/clusterBalloonOpen.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/cluster/clusterBalloonOpen.html')

            // Подождём карту и балун
            .waitReady()
            .waitForVisible(PO.map.balloon.twoColumnsTabs());
    });

    it('Открываем балун кластера с выбранной меткой', function () {
        return this.browser
            // Проверим скриншот
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();

    });
});
