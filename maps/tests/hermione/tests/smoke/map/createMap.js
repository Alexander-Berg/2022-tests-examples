describe('smoke/map/createMap.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/map/createMap.html', {domain: 'net'})
            .waitForVisible('body #toggle')
            .click('body #toggle');
    });

    it('Покажем и скроем карты', function () {
        return this.browser
            .waitReady()
            .pointerClick('body #toggle')
            .verifyNoErrors();
    });

    it.skip('Скроем карту с саджестом и результатами поиска топонима', function () {
        return this.browser
            .waitAndClick(PO.map.controls.search.large.input())
            .keys('Москва')
            .waitForVisible(PO.suggest.item0())
            .pointerClick(PO.suggest.item0())
            .waitForVisible(PO.map.balloon.closeButton())
            .pointerClick(PO.map.controls.search.large.input())
            .waitForVisible(PO.suggest.item0())
            .click('body #toggle')
            .verifyNoErrors();
    });

    it.skip('Скроем карту с поиском ППО, фулскрином', function () {
        return this.browser
            .waitAndClick(PO.map.controls.fullscreen())
            .waitAndClick(PO.map.controls.search.large.input(), 10, 10)
            .keys('Кафе')
            .waitForVisible(PO.suggest.item0())
            .pointerClick(PO.suggest.item0())
            .waitAndPointerClick(PO.map.placemark.placemark())
            .waitForVisible(PO.map.balloon.closeButton())
            .click('body #toggle')
            .verifyNoErrors();
    });

    it.skip('Скроем карту с объектным саджестом, спутником и линейкой', function () {
        return this.browser
            .waitAndClick(PO.map.controls.fullscreen())
            .pointerClick(PO.map.controls.ruler())
            .pointerClick(453, 233)
            .pointerClick(417, 392)
            .pointerClick(317, 200)
            .pointerClick(484, 325)
            .waitAndClick(PO.map.controls.listbox.typeSelectorIcon())
            .waitAndClick(PO.mapControlsListboxItem() + '=Спутник')
            .waitAndClick(PO.map.controls.search.large.input())
            .waitForVisible(PO.suggest.catalogItem())
            .waitAndClick('body #toggle')
            .verifyNoErrors();
    });
});
