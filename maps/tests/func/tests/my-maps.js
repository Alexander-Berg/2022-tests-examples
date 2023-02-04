describe('newConstructor/myMaps', () => {
    it('case 1', function () {
        return this.browser
            .wdtOpen('sid=9M3rTOWGfds_zYlVLD9nsMkRm87Wvsrm&width=500&height=400&lang=ru_RU&sourceType=mymaps')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.areasPane())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'case01');
    });

    it('case 2', function () {
        return this.browser
            .wdtOpen('sid=COrk8xE8bLrZ4VhGB0t8Z9Bvqv4CwoPI&width=500&height=400&lang=ru_RU&sourceType=mymaps')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.areasPane())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'case02');
    });

    it('case 3', function () {
        return this.browser
            .wdtOpen('sid=71xfa4tQxkTIk_QZRNTtqCt1s1n45FhW&width=500&height=400&lang=ru_RU&sourceType=mymaps')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.areasPane())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'case03');
    });

    it('case 4', function () {
        return this.browser
            .wdtOpen('sid=IMUpE49l0vbOgnXjwR8-53-sz0AgAlw-&width=500&height=400&lang=ru_RU&sourceType=mymaps')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.placemark())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'case04');
    });

    it('case 5', function () {
        return this.browser
            .wdtOpen('sid=VE1m7d_gzsHZz_pw5sywxBVvSzmWGyE1&width=500&height=400&lang=ru_RU&sourceType=mymaps')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.placemark())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'case05');
    });

    it('case 6', function () {
        return this.browser
            .wdtOpen('sid=djIZ3tuJtGPHf7KdeoiE_qzivmIcKTGO&width=500&height=400&lang=ru_RU&sourceType=mymaps')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.areasPane())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'case06');
    });

    it('case 7', function () {
        return this.browser
            .wdtOpen('sid=GLcx4uiSLGalB-cQ6KlxLMe-Xqmb1hTf&width=500&height=400&lang=ru_RU&sourceType=mymaps')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.placemark())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'case07');
    });

    it('case 8', function () {
        return this.browser
            .wdtOpen('sid=yfh4cOu-CqaMzR7hFMEl3zR6oIQOignb&width=500&height=400&lang=ru_RU&sourceType=mymaps')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'case08');
    });

    it('case 9', function () {
        return this.browser
            .wdtOpen('sid=m8sCVUjfPr2vnt8RR_8eah-9VRDXSW0s&width=500&height=400&lang=ru_RU&sourceType=mymaps')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.placemark())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'case09');
    });

    it('case 10', function () {
        return this.browser
            .wdtOpen('sid=4ybg7nz7xaqzUN-_6ysAP31kh2iEa0xO&width=500&height=400&lang=ru_RU&sourceType=mymaps')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.placemark())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'case10');
    });

    it('case 11', function () {
        return this.browser
            .wdtOpen('sid=4ybg7nz7xaqzUN-_6ysAP31kh2iEa0xO&width=500&height=400&lang=ru_RU&sourceType=mymaps')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.placemark())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'case11');
    });

    it('case 12', function () {
        return this.browser
            .wdtOpen('sid=D3g-3Vt7FtK6ThdTwGfFn_96GaG_xeH_&width=500&height=400&lang=ru_RU&sourceType=mymaps')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.placemark())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'case12');
    });
});
