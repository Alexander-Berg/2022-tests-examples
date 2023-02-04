describe('balloon/setData.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/setData.html')
            .waitReady();
    });

    it('Проверяем методы', function () {
        return this.browser
            .waitForVisible(PO.map.balloon.content())
            .csCheckText('body #logger', 'Expected values - true\nOK\ndata:\nOK\ndata (html):\nOK')
            .verifyNoErrors();
    });
});