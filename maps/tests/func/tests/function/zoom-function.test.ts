import {openStudio, clickToSelector, waitForSelector, getNodeChildWithSelector} from '../../utils/commands';

const SELECTORS = {
    openFunctionViewButton: '[data-setting="line-width"] .property-view__stops-button',
    functionPropertyView: '.function-property-view',
    chartView: '.function-property-view .chart-view',
    typeSelectView: '.function-property-view .stops-type-control__control',
    stopsView: '.function-property-view .zoom-stops-view',
    zoomStop: {
        view: '.zoom-stop-item',
        valueInput: '.zoom-stop-item__property .discrete-input input.input__control',
        increaseValueButton: '.zoom-stop-item__property .discrete-input .discrete-input__increase',
        deleteButton: '.zoom-stop-item .zoom-stop-item__delete-button'
    },
    addNewStopButton: '.zoom-stops-view .zoom-stops-view__add-zoom'
};
const TYPE_VALUE = '12';
const INCREASE_STEP = 0.1;

// https://testpalm.yandex-team.ru/testcase/gryadka-107
describe('Function style property', () => {
    beforeEach(async () => {
        await openStudio();
        await clickToSelector(SELECTORS.openFunctionViewButton);
        await waitForSelector(SELECTORS.functionPropertyView);
    });

    test('should show function panel', () => {
        return Promise.all([SELECTORS.chartView, SELECTORS.typeSelectView, SELECTORS.stopsView].map(waitForSelector));
    });

    test('change zoom function item value by typing', async () => {
        const [zoomItem] = await page.$$(SELECTORS.zoomStop.view);
        const valueInput = await getNodeChildWithSelector(zoomItem, SELECTORS.zoomStop.valueInput);
        await valueInput!.evaluate((node, value) => node.setAttribute('value', value), TYPE_VALUE);
        const nextValue = await valueInput!.evaluate((node) => node.getAttribute('value'));

        expect(nextValue === TYPE_VALUE);
    });

    test('change zoom function item value by button', async () => {
        const [, zoomItem] = await page.$$(SELECTORS.zoomStop.view);
        const valueInput = await getNodeChildWithSelector(zoomItem, SELECTORS.zoomStop.valueInput);
        const prevInputValue = await valueInput!.evaluate((node) => node.getAttribute('value'));
        const valueIncreaseButton = await getNodeChildWithSelector(zoomItem, SELECTORS.zoomStop.increaseValueButton);
        await valueIncreaseButton!.click();
        const nextInputValue = await valueInput!.evaluate((node) => node.getAttribute('value'));

        expect(Number(prevInputValue) + INCREASE_STEP === Number(nextInputValue));
    });

    test('add new zoom item', async () => {
        const prevItemsCount = (await page.$$(SELECTORS.zoomStop.view)).length;
        await clickToSelector(SELECTORS.addNewStopButton);
        const nextItemsCount = (await page.$$(SELECTORS.zoomStop.view)).length;

        expect(prevItemsCount + 1 === nextItemsCount);
    });

    test('remove zoom item', async () => {
        const prevItemsCount = (await page.$$(SELECTORS.zoomStop.view)).length;
        await clickToSelector(SELECTORS.addNewStopButton);
        await clickToSelector(SELECTORS.zoomStop.deleteButton);
        const nextItemsCount = (await page.$$(SELECTORS.zoomStop.view)).length;

        expect(prevItemsCount === nextItemsCount);
    });
});
