const MAP = 'um=constructor%3Afc3c5adb9c54f75a4ba02cb78069873ced7d7f7ad4fab30f6b6983579caa561f&amp;lang=ru_RU&amp;';

describe('newConstructor/sizes', () => {
    const prepare = function () {
        return this
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.areasPane());
    };

    it('width=100% height=250', function () {
        return this.browser
            .wdtOpen(MAP + 'width=100%&amp;height=250')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), '100per-250px');
    });

    it('width=800 height=700', function () {
        return this.browser
            .wdtOpen(MAP + 'width=800&amp;height=700')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), '800px-700px');
    });

    it('width=500 height=400', function () {
        return this.browser
            .wdtOpen(MAP + 'width=500&amp;height=400')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), '500px-400px');
    });

    it('width=500 height=100', function () {
        return this.browser
            .wdtOpen(MAP + 'width=500&amp;height=100')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), '500px-100px');
    });

    it('width=410 height=240', function () {
        return this.browser
            .wdtOpen(MAP + 'width=410&amp;height=240')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), '410px-240px');
    });

    it('width=400 height=400', function () {
        return this.browser
            .wdtOpen(MAP + 'width=400&amp;height=400')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), '400px-400px');
    });

    it('width=400 height=300', function () {
        return this.browser
            .wdtOpen(MAP + 'width=400&amp;height=300')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), '400px-300px');
    });

    it('width=320 height=240', function () {
        return this.browser
            .wdtOpen(MAP + 'width=320&amp;height=240')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), '320px-240px');
    });

    it('width=320 height=239', function () {
        return this.browser
            .wdtOpen(MAP + 'width=320&amp;height=239')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), '320px-239px');
    });

    it('width=319 height=240', function () {
        return this.browser
            .wdtOpen(MAP + 'width=319&amp;height=240')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), '319px-240px');
    });

    it('width=300 height=300', function () {
        return this.browser
            .wdtOpen(MAP + 'width=300&amp;height=300')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), '300px-300px');
    });

    it('width=300 height=100', function () {
        return this.browser
            .wdtOpen(MAP + 'width=300&amp;height=100')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), '300px-100px');
    });

    it('width=250 height=250', function () {
        return this.browser
            .wdtOpen(MAP + 'width=250&amp;height=250')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), '250px-250px');
    });

    it('width=200 height=200', function () {
        return this.browser
            .wdtOpen(MAP + 'width=200&amp;height=200')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), '200px-200px');
    });

    it('width=170 height=170', function () {
        return this.browser
            .wdtOpen(MAP + 'width=170&amp;height=170')
            .then(prepare)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), '170px-170px');
    });
});
