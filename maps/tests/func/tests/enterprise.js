describe('newConstructor/apikeyCase', () => {
    hermione.skip.in('ie11', 'С enterprise виджет не работает в IE');
    it('case 1', function () {
        return this.browser
            .wdtOpen('sid=qS4b5gHJY2nHnkDXcMpjqtgSdYeFdKxk&width=320&height=240&lang=en_US&sourceType=mymaps' +
                '&apikey=b027f76e-cc66-f012-4f64-696c7961c395', {isEnterprise: true})
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.areasPane())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'case1');
    });

    hermione.skip.in('ie11', 'С enterprise виджет не работает в IE');
    it('case 2', function () {
        return this.browser
            .wdtOpen('sid=gNNjsb_yY2I1xMz4jqP_O5PIVTdUp5Fp&width=350&height=250' +
                '&apikey=b027f76e-cc66-f012-4f64-696c7961c395&lang=ru_RU', {isEnterprise: true})
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.placemark())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'case2');
    });

    it('case 3', function () {
        return this.browser
            .wdtOpen('sid=qS4b5gHJY2nHnkDXcMpjqtgSdYeFdKxk&width=500&height=500&lang=tk_TR&sourceType=mymaps' +
                '&apikey=b027f76e-cc66-f012-4f64-696c7961c395')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.areasPane())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'case3');
    });
});
