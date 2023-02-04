import {openPage, click, overwrite} from '../../utils/commands';
import cssSelectors from '../../common/css-selectors';

describe('Attribute input', () => {
    beforeEach(async () => {
        await openPage({useAuth: true});
        await click(cssSelectors.shipments.shipment);
        await click(cssSelectors.panoramas.panorama);
    });

    test('with type date should correctly place delimeters', async () => {
        const {date, time} = cssSelectors.attributesEditor.dateTimeInput;
        const typedDate = '25121991';
        const expectedDate = '25.12.1991';
        await overwrite(date, typedDate);
        const actualDate = await page.$eval(date, (el: HTMLInputElement) => el.value);
        expect(actualDate).toEqual(expectedDate);

        const typedTime = '120530';
        const expectedTime = '12:05:30';
        await overwrite(time, typedTime);
        const actualTime = await page.$eval(time, (el: HTMLInputElement) => el.value);
        expect(actualTime).toEqual(expectedTime);
    });
});
