describe('newConstructor/404error', () => {
    hermione.skip.notIn('', 'MAPSCNSTWIDGET-83: Не появляется заглушка удаленной карты, если скрипт был добавлен после создания страницы')
    it('width=100% height=400', function () {
        return this.browser
            .wdtOpen('um=constructor%3A0058ebfb5cd18a1edf6fba85c82f139994aa22f77b769949d0d1f5109d2ead16&amp;' +
                'width=100%25&amp;height=400&amp;lang=ru_RU&amp;scroll=true')
            .wdtWaitForVisible(PO.errorCap())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), '100per-400px');
    });
});
