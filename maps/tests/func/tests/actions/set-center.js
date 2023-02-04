const {WAIT_FOR_UPDATE, WAIT_FOR_UPDATE_SHORT} = require('../../constants');
const zoom = 13;
const centres = [
    [53.902198, 27.559205],
    [55.00081759, 82.95627700],
    [59.437210, 24.745527]
];

describe('Actions -setCenter', () => {
    it('case-1', async ({browser}) => {
        await browser.openMap({center: centres[0], zoom: zoom});
        await browser.execute(addSetCenterControls, centres).catch((err) => new Error(err));
        await browser.verifyNoErrors();
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyScreenshot('setCenter-1-1', PO.map());
        await browser.deleteTilesLoaded();
        await browser.click(PO.map.controls.button1());
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyScreenshot('setCenter-1-2', PO.map());
        await browser.deleteTilesLoaded();
        await browser.click(PO.map.controls.button2());
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyScreenshot('setCenter-1-3', PO.map());
        await browser.verifyNoErrors();
    });

    it('case-2', async ({browser}) => {
        await browser.openMap({center: centres[0], zoom: zoom});
        await browser.execute(addSetCenterControls, centres).catch((err) => new Error(err));
        await browser.verifyNoErrors();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.waitForTilesLoaded();
        await browser.click(PO.map.controls.button1());
        await browser.pause(WAIT_FOR_UPDATE_SHORT);
        await browser.click(PO.map.controls.button2());
        await browser.pause(WAIT_FOR_UPDATE_SHORT);
        await browser.click(PO.map.controls.button1());
        await browser.pause(WAIT_FOR_UPDATE_SHORT);
        await browser.click(PO.map.controls.button2());
        await browser.pause(WAIT_FOR_UPDATE_SHORT);
        await browser.click(PO.map.controls.button1());
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyScreenshot('setCenter-2-1', PO.map());
        await browser.verifyNoErrors();
    });

    function addSetCenterControls(centres) {
        const novosibirskBtn = new ymaps.control.Button({
            data: {
                content: 'Новосибирск'
            },
            options: {
                selectOnClick: false
            }
        });
        novosibirskBtn.events.add('click', () => myMap.setCenter(centres[1]));

        const tallinnBtn = new ymaps.control.Button({
            data: {
                content: 'Таллин'
            },
            options: {
                selectOnClick: false
            }
        });
        tallinnBtn.events.add('click', () => myMap.setCenter(centres[2]));

        myMap.controls
            .add(novosibirskBtn)
            .add(tallinnBtn);

        myMap.geoObjects
            .add(new ymaps.Placemark(centres[0]))
            .add(new ymaps.Placemark(centres[1]))
            .add(new ymaps.Placemark(centres[2]));

        return true;
    }
});
