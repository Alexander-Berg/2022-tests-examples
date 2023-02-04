import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import Gate from 'realty-core/view/react/libs/gate';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import BackCallForm from '../';

const emptyInitialState = {
    user: {
        crc: '123'
    },
    backCallApi: {
        SITE: {},
        VILLAGE: {},
        DEVELOPER: {}
    }
};
const successPhone = '+79871234455';

const makeCallMock = (action, { phone }) => phone === successPhone ? Promise.resolve() : Promise.reject();

const GateDefault = Gate;

Gate.create = (action, ...args) => action === 'back-call.makeCallTeleponyV2' ?
    makeCallMock(action, ...args) :
    GateDefault.create(action, ...args);

describe('BackCallForm', () => {
    it('рисует форму обратного звонка без введеного номера телефона, с заблокированой кнопкой', async() => {
        await render(
            <AppProvider initialState={emptyInitialState}>
                <BackCallForm siteId='123' />
            </AppProvider>,
            { viewport: { width: 400, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует форму обратного звонка с предзаполненым номером телефона, с разблокированой кнопкой', async() => {
        const initialState = {
            user: {
                crc: '123',
                defaultPhone: '+79998887766'
            }
        };

        await render(
            <AppProvider initialState={initialState}>
                <BackCallForm siteId='123' />
            </AppProvider>,
            { viewport: { width: 400, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует пустое поле ввода при попытке ввода не числовых симовлов', async() => {
        await render(
            <AppProvider initialState={emptyInitialState}>
                <BackCallForm siteId='123' />
            </AppProvider>,
            { viewport: { width: 400, height: 200 } }
        );

        const input = await page.$('.TextInput__control');

        await input.type('qwertt');
        await input.type('$%^&*');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует заблокированую кнопку после частичного ввода номера телефона', async() => {
        await render(
            <AppProvider initialState={emptyInitialState}>
                <BackCallForm siteId='123' />
            </AppProvider>,
            { viewport: { width: 400, height: 200 } }
        );

        const input = await page.$('.TextInput__control');

        await input.type('9001112');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует разблокированую кнопку после ввода полного номера телефона', async() => {
        await render(
            <AppProvider initialState={emptyInitialState}>
                <BackCallForm siteId='123' />
            </AppProvider>,
            { viewport: { width: 400, height: 200 } }
        );

        const input = await page.$('.TextInput__control');

        await input.type('79001112233');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует экран с введеным номером после успешного заказа', async() => {
        const succcessInitialState = {
            backCallApi: {
                SITE: { 123: { sourceNumber: '+79998887766' } }
            }
        };

        await render(
            <AppProvider initialState={succcessInitialState}>
                <BackCallForm siteId='123' />
            </AppProvider>,
            { viewport: { width: 400, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
