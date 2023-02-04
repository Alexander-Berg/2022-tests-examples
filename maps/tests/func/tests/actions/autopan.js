const zoom = 17;
const {WAIT_FOR_UPDATE} = require('../../constants');
const cs = {
    name: 'autoPan',
    coords: [55.75735630854855, 37.61503342587282],
    autoPan: [59.467741, 28.038986],
    autoPan1: [59.925701, 30.396064],
    autoPan2: [60.027050, 28.342665]
};

describe('Actions -autoPan', () => {
    it(cs.name, async ({browser}) => {
        await browser.openMap({center: cs.coords, zoom: zoom});
        await browser.execute(addAutoPanControls, cs).catch((err) => new Error(err));
        await browser.verifyNoErrors();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyScreenshot(cs.name + '-1', PO.map());
        await browser.deleteTilesLoaded();
        await browser.click(PO.map.controls.button1());
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyScreenshot(cs.name + '-2', PO.map());
        await browser.deleteTilesLoaded();
        await browser.click(PO.map.controls.button2());
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.waitForTilesLoaded();
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyScreenshot(cs.name + '-3', PO.map());
        await browser.verifyNoErrors();
    });

    function addAutoPanControls(cs) {
        const balloon = new ymaps.Balloon(myMap);
        const contentLayout = ymaps.templateLayoutFactory.createClass('$[balloonContent]balloon');
        const options = {
            contentLayout: contentLayout,
            closeButton: false,
            shadow: false
        };

        balloon.options.set(options).setParent(myMap.options);
        balloon.open(cs.autoPan, true);

        balloon.options.set('autoPanDuration', 2500);
        balloon.options.set('autoPanMargin', 100);

        const positionButton = new ymaps.control.Button({
            data: {
                content: cs.autoPan1.join(', ')
            },
            options: {
                selectOnClick: false
            }
        });
        positionButton.events.add('click', () => {
            balloon.setPosition(cs.autoPan1);
        });

        const anotherPositionButton = new ymaps.control.Button({
            data: {
                content: cs.autoPan2.join(', ')
            },
            options: {
                selectOnClick: false
            }
        });
        anotherPositionButton.events.add('click', () => {
            balloon.setPosition(cs.autoPan2);
        });

        myMap.controls
            .add(positionButton)
            .add(anotherPositionButton);

        return true;
    }
});
