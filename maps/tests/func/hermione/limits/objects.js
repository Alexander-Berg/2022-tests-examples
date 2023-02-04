require('../helper.js')(afterEach);

describe.skip('Create large count of placemarks', () => {
    let count = 1;
    const createPlacemark = function () {
        return this.browser
            .crClickOnMap();
    };

    before(function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crWaitForVisible(PO.addPlacemark(), 'Не появился контрол добавления метки')
            .click(PO.addPlacemark());
    });

    while (--count) {
        it('Create placemark ' + count, createPlacemark);
    }

    it('Save', function () {
        return this.browser
            .click(PO.saveAndContinue())
            .debug();
    });
});
