import {openPage, wait, click} from '../../utils/commands';
import cssSelectors from '../../common/css-selectors';

describe('Panorama player', () => {
    beforeEach(async () => {
        await openPage({useAuth: true});
        await click(cssSelectors.shipments.shipment);
        await click(cssSelectors.panoramas.panorama);
        await page.mouse.click(650, 200, {button: 'right'});
    });

    test('should default direction properly', async () => {
        const defaultDirectionExpected = 357.71;
        await page.waitForSelector(cssSelectors.player.container);
        await wait(2000);
        await page.mouse.move(600, 700);
        await page.mouse.down();
        await page.mouse.move(610, 700);
        await page.mouse.up();
        await click(cssSelectors.player.setDefaultDirectionButton);
        const defaultDirectionActual = await page.$$eval(cssSelectors.attributesEditor.numericInput,
            (elements: HTMLInputElement[]) => Number(elements[5].value)
        );
        expect(defaultDirectionActual).toBeCloseTo(defaultDirectionExpected, 1);
    });

    test('should set north properly', async () => {
        const northExpected = 280;
        await page.waitForSelector(cssSelectors.player.container);
        await wait(2000);
        await click(cssSelectors.player.setNorthButton);
        await page.mouse.click(700, 200);
        await wait(2000);
        const northActual = await page.$$eval(cssSelectors.attributesEditor.numericInput,
            (elements: HTMLInputElement[]) => Number(elements[3].value)
        );
        expect(northActual).toBeCloseTo(northExpected, 1);
    });
});
