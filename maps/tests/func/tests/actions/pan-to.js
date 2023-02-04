const zoom = 12;
const {WAIT_FOR_UPDATE} = require('../../constants');

describe('Actions -panTo', () => {
    it('panTo', async ({browser}) => {
        await browser.openMap({center: [55.72, 37.64], zoom: zoom});
        await browser.verifyNoErrors();
        await browser.execute(addPanToControls).catch((err) => new Error(err));
        await browser.verifyNoErrors();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyScreenshot('panTo-1', PO.map());
        await browser.deleteTilesLoaded();
        await browser.click(PO.map.controls.button1());
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyScreenshot('panTo-2', PO.map());
        await browser.deleteTilesLoaded();
        await browser.click(PO.map.controls.button2());
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyScreenshot('panTo-3', PO.map());
        await browser.deleteTilesLoaded();
        await browser.click(PO.map.controls.button3());
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyScreenshot('panTo-4', PO.map());
        await browser.deleteTilesLoaded();
        await browser.click(PO.map.controls.button4());
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyScreenshot('panTo-5', PO.map());
        await browser.verifyNoErrors();
    });

    function addPanToControls() {
        const kazanButton = new ymaps.control.Button({
            data: {
                content: 'Казань'
            },
            options: {
                selectOnClick: false
            }
        });
        kazanButton.events.add('click', () => {
            myMap.panTo([
                [54.311505, 48.326255],
                [55.794251, 49.105726]
            ], {
                flying: true,
                delay: 2000
            });
        });

        const mskButton = new ymaps.control.Button({
            data: {
                content: 'Москва'
            },
            options: {
                selectOnClick: false
            }
        });
        mskButton.events.add('click', () => {
            myMap.panTo([55.749394, 37.622341], {
                flying: true
            });
        });

        const spbButton = new ymaps.control.Button({
            data: {
                content: 'Петербург'
            },
            options: {
                selectOnClick: false
            }
        });
        spbButton.events.add('click', () => {
            myMap.panTo([59.939280, 30.319961], {
                flying: false,
                duration: 2000
            });
        });

        const nycButton = new ymaps.control.Button({
            data: {
                content: 'Нью-Йорк'
            },
            options: {
                selectOnClick: false
            }
        });
        nycButton.events.add('click', () => {
            myMap.panTo([
                [40.340177, -3.747104],
                [40.695396170656885, -73.74560661546253]
            ], {
                flying: true,
                duration: 1000,
                delay: 500
            });
        });

        myMap.controls
            .add(kazanButton)
            .add(mskButton)
            .add(spbButton)
            .add(nycButton);

        myMap.geoObjects
            .add(new ymaps.Placemark([55.794251, 49.105726]))
            .add(new ymaps.Placemark([55.749394, 37.622341]))
            .add(new ymaps.Placemark([59.939280, 30.319961]))
            .add(new ymaps.Placemark([40.695396170656885, -73.74560661546253]));

        return true;
    }
});
