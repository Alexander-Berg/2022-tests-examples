import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';
import cloneDeep from 'lodash/cloneDeep';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/lib/test-helpers';

import PaySelector from '../index';

import { getState, defaultProducts, productsWithDiscount } from './mocks';

advanceTo(new Date('2020-10-05 13:57:00'));

const context = {
    scroll: () => {}
};

const Component = (props = {}) => {
    const { initialState } = props;
    let { Gate } = props;

    Gate = Gate || {
        create: () => {
            return Promise.resolve(defaultProducts);
        }
    };

    return (
        <AppProvider initialState={initialState} context={context} Gate={Gate}>
            <PaySelector
                isTrap={false}
                callback={() => {}}
                paymentId='1'
                link={() => {}}
                {...props}
            />
        </AppProvider>
    );
};

describe('Страница подачи/редактирования. PaySelector', () => {
    it('Подача, продажа квартиры, нет адреса, нет фото', async() => {
        const state = getState();

        state.offerForm.category = 'APARTMENT';
        state.offerForm.location = undefined;
        state.offerForm.photo = undefined;

        const component = (
            <Component initialState={state} />
        );

        await render(component, { viewport: { width: 1000, height: 600 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Подача, продажа квартиры, есть адрес, нет фото', async() => {
        const state = getState();

        state.offerForm.category = 'APARTMENT';
        state.offerForm.photo = undefined;

        const component = (
            <Component initialState={state} />
        );

        await render(component, { viewport: { width: 1000, height: 600 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Подача, продажа квартиры, есть адрес, есть 1 фото', async() => {
        const state = getState();

        state.offerForm.category = 'APARTMENT';
        state.offerForm.photo = [ '' ];

        const component = (
            <Component initialState={state} />
        );

        await render(component, { viewport: { width: 1000, height: 600 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Подача, продажа квартиры, есть адрес, есть 4 фото', async() => {
        const state = getState();

        state.offerForm.category = 'APARTMENT';
        state.offerForm.photo = [ '', '', '', '' ];

        const component = (
            <Component initialState={state} />
        );

        await render(component, { viewport: { width: 1000, height: 1000 } });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Подача, продажа коммерческой, есть адрес, есть 1 фото', async() => {
        const state = getState();

        state.offerForm.category = 'COMMERCIAL';
        state.offerForm.photo = [ '' ];

        const component = (
            <Component initialState={state} />
        );

        await render(component, { viewport: { width: 1000, height: 600 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Подача, продажа коммерческой, есть адрес, есть 2 фото', async() => {
        const state = getState();

        state.offerForm.category = 'COMMERCIAL';
        state.offerForm.photo = [ '', '' ];

        const component = (
            <Component initialState={state} />
        );

        await render(component, { viewport: { width: 1000, height: 1000 } });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Редактирование, продажа квартиры, нет адреса, нет фото', async() => {
        const state = getState();

        state.offerForm.category = 'APARTMENT';
        state.offerForm.location = undefined;
        state.offerForm.photo = undefined;
        state.offerForm._isEdit = true;

        const component = (
            <Component initialState={state} />
        );

        await render(component, { viewport: { width: 1000, height: 600 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Редактирование, продажа квартиры, есть адрес, нет фото', async() => {
        const state = getState();

        state.offerForm.category = 'APARTMENT';
        state.offerForm.photo = undefined;
        state.offerForm._isEdit = true;

        const component = (
            <Component initialState={state} />
        );

        await render(component, { viewport: { width: 1000, height: 600 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Редактирование, продажа квартиры, есть адрес, есть 4 фото', async() => {
        const state = getState();

        state.offerForm.category = 'APARTMENT';
        state.offerForm.photo = [ '', '', '', '' ];
        state.offerForm._isEdit = true;

        const component = (
            <Component initialState={state} />
        );

        await render(component, { viewport: { width: 1000, height: 600 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Подача, продажа квартиры, нет адреса, нет фото, юр. лицо', async() => {
        const state = getState();

        state.offerForm.category = 'APARTMENT';
        state.offerForm.location = undefined;
        state.offerForm.photo = undefined;
        state.offerForm._isEdit = false;
        state.accountForm.userType = 'AGENCY';
        state.user.isJuridical = true;
        state.vosUserData.paymentType = 'JURIDICAL_PERSON';
        state.vosUserData.userType = 'AGENCY';

        const component = (
            <Component initialState={state} />
        );

        await render(component, { viewport: { width: 1000, height: 600 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Подача, продажа квартиры, есть адреса, нет фото, юр. лицо', async() => {
        const state = getState();

        state.offerForm.category = 'APARTMENT';
        state.offerForm.photo = undefined;
        state.offerForm._isEdit = false;
        state.accountForm.userType = 'AGENCY';
        state.user.isJuridical = true;
        state.vosUserData.paymentType = 'JURIDICAL_PERSON';
        state.vosUserData.userType = 'AGENCY';

        const component = (
            <Component initialState={state} />
        );

        await render(component, { viewport: { width: 1000, height: 600 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Подача, продажа квартиры, есть адреса, есть 4 фото, юр. лицо', async() => {
        const state = getState();

        state.offerForm.category = 'APARTMENT';
        state.offerForm.photo = [ '', '', '', '' ];
        state.offerForm._isEdit = false;
        state.accountForm.userType = 'AGENCY';
        state.user.isJuridical = true;
        state.vosUserData.paymentType = 'JURIDICAL_PERSON';
        state.vosUserData.userType = 'AGENCY';

        const component = (
            <Component initialState={state} />
        );

        await render(component, { viewport: { width: 1000, height: 600 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Редактирование, продажа квартиры, есть адреса, нет фото, юр. лицо', async() => {
        const state = getState();

        state.offerForm.category = 'APARTMENT';
        state.offerForm.photo = undefined;
        state.offerForm._isEdit = true;
        state.accountForm.userType = 'AGENCY';
        state.user.isJuridical = true;
        state.vosUserData.paymentType = 'JURIDICAL_PERSON';
        state.vosUserData.userType = 'AGENCY';

        const component = (
            <Component initialState={state} />
        );

        await render(component, { viewport: { width: 1000, height: 600 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Редактирование, продажа квартиры, есть адреса, есть 4 фото, юр. лицо', async() => {
        const state = getState();

        state.offerForm.category = 'APARTMENT';
        state.offerForm.photo = [ '', '', '', '' ];
        state.offerForm._isEdit = true;
        state.accountForm.userType = 'AGENCY';
        state.user.isJuridical = true;
        state.vosUserData.paymentType = 'JURIDICAL_PERSON';
        state.vosUserData.userType = 'AGENCY';

        const component = (
            <Component initialState={state} />
        );

        await render(component, { viewport: { width: 1000, height: 600 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

describe('Страница подачи/редактирования. Скидки', () => {
    it('Скидки на VAS', async() => {
        const state = getState();

        state.offerForm.category = 'APARTMENT';
        state.offerForm.photo = [ '', '', '', '' ];

        const Gate = {
            create: () => Promise.resolve(productsWithDiscount)
        };

        const component = (
            <Component initialState={state} Gate={Gate} />
        );

        await render(component, { viewport: { width: 1000, height: 1000 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click('ul > li:nth-child(2)');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Скидки на Размещение', async() => {
        const state = getState();

        state.offerForm.category = 'APARTMENT';
        state.offerForm.photo = [ '', '', '', '' ];
        state.discountInfo = {
            placement: {
                endDate: '2020-10-06T15:20:00.111Z',
                percent: 75
            }
        };

        const component = (
            <Component initialState={state} />
        );

        await render(component, { viewport: { width: 1000, height: 1000 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click('ul > li:nth-child(2)');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Скидки на Размещение, есть квота', async() => {
        const state = getState();

        state.offerForm.category = 'APARTMENT';
        state.offerForm.photo = [ '', '', '', '' ];
        state.discountInfo = {
            placement: {
                endDate: '2020-10-06T15:20:00.111Z',
                percent: 75
            }
        };

        const quotaProducts = cloneDeep(defaultProducts);

        quotaProducts.individualCost.placement.placement = { quota: {} };

        const Gate = {
            create: () => Promise.resolve(quotaProducts)
        };

        const component = (
            <Component initialState={state} Gate={Gate} />
        );

        await render(component, { viewport: { width: 1000, height: 1000 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click('ul > li:nth-child(2)');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Долгосрочная скидка на Размещение', async() => {
        const state = getState();

        state.offerForm.category = 'APARTMENT';
        state.offerForm.photo = [ '', '', '', '' ];
        state.discountInfo = {
            placement: {
                endDate: '2020-10-06T15:20:00.111Z',
                amount: 1
            }
        };

        const component = (
            <Component initialState={state} />
        );

        await render(component, { viewport: { width: 1000, height: 1000 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click('ul > li:nth-child(2)');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
