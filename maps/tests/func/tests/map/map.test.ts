import {openPage, wait, click} from '../../utils/commands';
import cssSelectors from '../../common/css-selectors';

describe('Map', () => {
    beforeEach(async () => {
        await openPage({useAuth: true});
        await click(cssSelectors.shipments.shipment);
        await click(cssSelectors.panoramas.panorama);
    });

    test('should set coordinates on right click', async () => {
        await click(cssSelectors.map, {button: 'right'});
        await wait(300);
        const actual = await page.$$eval(cssSelectors.attributesEditor.numericInput,
            (elements: HTMLInputElement[]) => elements[1].value
        );
        expect(actual).not.toEqual('');
    });
});
