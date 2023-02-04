describe('smoke/objectManager/filter.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/objectManager/filter.html')
            .waitReady();
    });

    it('Поочерёдно удаляем все результаты и добавляем их вновь', function () {
        return this.browser
            .waitAndClick(PO.map.controls.listbox.selectedItem())
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'results')
            .waitAndClick(PO.map.controls.listbox.selectedItem())
            .waitAndClick(PO.map.controls.listbox.selectedItem())
            .waitAndClick(PO.map.controls.listbox.selectedItem())
            .waitAndClick(PO.map.controls.listbox.selectedItem())
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .waitAndClick(PO.map.controls.listbox.nonSelectedItem())
            .waitAndClick(PO.map.controls.listbox.nonSelectedItem())
            .waitAndClick(PO.map.controls.listbox.nonSelectedItem())
            .waitAndClick(PO.map.controls.listbox.nonSelectedItem())
            .waitAndClick(PO.map.controls.listbox.nonSelectedItem())
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'anotherResults')
            .verifyNoErrors();
    });
});
