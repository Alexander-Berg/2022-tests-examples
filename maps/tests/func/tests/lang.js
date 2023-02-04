const MAP = 'sid=e7f1011f0df2b0b2421cdf012ba7746b8fa861e94f87d51428a29d1217a484e8&width=500&height=400' +
    '&sourceType=constructor';

describe('widget/lang', () => {
    const prepare = function () {
        return this
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.areasPane());
    };

    it('lang=ru_RU', function () {
        return this.browser
            .wdtOpen(MAP + '&lang=ru_RU')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'ru_RU');
    });

    it('lang=en_US', function () {
        return this.browser
            .wdtOpen(MAP + '&lang=en_US')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'en_US');
    });

    it('lang=en_RU', function () {
        return this.browser
            .wdtOpen(MAP + '&lang=en_RU')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'en_RU');
    });

    it('lang=uk_UA', function () {
        return this.browser
            .wdtOpen(MAP + '&lang=uk_UA')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'uk_UA');
    });

    it('lang=ru_UA', function () {
        return this.browser
            .wdtOpen(MAP + '&lang=ru_UA')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'ru_UA');
    });

    it('lang=tr_TR', function () {
        return this.browser
            .wdtOpen(MAP + '&lang=tr_TR')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'tr_TR');
    });

    it('lang=ru-RU', function () {
        return this.browser
            .wdtOpen(MAP + '&lang=ru-RU')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'ru-RU');
    });

    it('lang=en-US', function () {
        return this.browser
            .wdtOpen(MAP + '&lang=en-US')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'en-US');
    });

    it('lang=uk-UA', function () {
        return this.browser
            .wdtOpen(MAP + '&lang=uk-UA')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'uk-UA');
    });

    it('lang=tr-Tr', function () {
        return this.browser
            .wdtOpen(MAP + '&lang=tr-Tr')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'tr-Tr');
    });

    it('lang="', function () {
        return this.browser
            .wdtOpen(MAP + '&lang=')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'empty');
    });

    it('без lang', function () {
        return this.browser
            .wdtOpen(MAP)
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'without');
    });
});
