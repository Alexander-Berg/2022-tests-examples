import React from 'react';
import { render } from 'jest-puppeteer-react';
import cloneDeep from 'lodash/cloneDeep';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { allure } from '@realty-front/jest-utils/puppeteer/tests-helpers/allure';

import Gate from 'realty-core/view/react/libs/gate';

import { AppProvider } from 'view/lib/test-helpers';

import { FinancesManagementBlockContainer } from '../container';

import styles from '../styles.module.css';
import actionButtonStyles from '../ActionButton/styles.module.css';
import creditHintStyles from '../ActionButton/CreditAdditional/styles.module.css';

import {
    getState,
    noPersonBillingRequisites,
    singlePersonBillingRequisites,
    twoPersonBillingRequisites,
    longNameBillingRequisites
} from './mocks';

const [ WIDTH, HEIGHT ] = [ 600, 400 ];

Gate.create = action => {
    if (action === 'billing.generate_invoice_pdf') {
        return new Promise(resolve => {
            setTimeout(() => resolve(), 500);
        });
    }

    return Promise.resolve();
};

const Component = ({ store, ...props }) => (
    <AppProvider initialState={store}>
        <FinancesManagementBlockContainer {...props} />
    </AppProvider>
);

describe('FinancesManagementBlock', () => {
    describe('Ошибки', () => {
        it('Ошибка загрузки кошелька', async() => {
            const store = getState({
                wallet: { status: 'errored' }
            });
            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Нет реквизитов в билленге', async() => {
            const store = getState();

            store.billingRequisites = noPersonBillingRequisites;

            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Оплата картой', () => {
        it('Несколько плательщиков, не выбран', async() => {
            const store = getState();
            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Один плательщик, не выбран', async() => {
            const store = getState();

            store.billingRequisites = singlePersonBillingRequisites;

            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Плательщик - физ. лицо', async() => {
            const store = getState();

            store.billingRequisites = cloneDeep(twoPersonBillingRequisites);
            store.billingRequisites.byId['12001231'].personType = 'ph';
            store.billingRequisites.byId['12001231'].lname = 'Цой';
            store.billingRequisites.byId['12001231'].fname = 'Виктор';
            store.billingRequisites.byId['12001231'].mname = 'Робертович';

            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Плательщик - физ. лицо, ховер на инфо-иконку', async() => {
            const store = getState();

            store.billingRequisites = cloneDeep(twoPersonBillingRequisites);
            store.billingRequisites.byId['12001231'].personType = 'ph';
            store.billingRequisites.byId['12001231'].lname = 'Цой';
            store.billingRequisites.byId['12001231'].fname = 'Виктор';
            store.billingRequisites.byId['12001231'].mname = 'Робертович';

            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            await page.hover(`.${actionButtonStyles.tip}`);
            await page.waitFor(300);

            expect(await takeScreenshot({
                keepCursor: true
            })).toMatchImageSnapshot();
        });

        it('Плательщик - юр. лицо, ховер на инфо-иконку', async() => {
            const store = getState();

            store.billingRequisites = cloneDeep(twoPersonBillingRequisites);

            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            await page.hover(`.${actionButtonStyles.tip}`);
            await page.waitFor(300);

            expect(await takeScreenshot({
                keepCursor: true
            })).toMatchImageSnapshot();
        });

        it('Плательщик - физ. лицо, без отчества', async() => {
            const store = getState();

            store.billingRequisites = cloneDeep(twoPersonBillingRequisites);
            store.billingRequisites.byId['12001231'].personType = 'ph';
            store.billingRequisites.byId['12001231'].lname = 'Булгаков';
            store.billingRequisites.byId['12001231'].fname = 'Михаил';

            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Длинное имя плательщика', async() => {
            const store = getState();

            store.billingRequisites = longNameBillingRequisites;

            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Несколько плательщиков, не задана сумма', async() => {
            const store = getState();
            const component = <Component store={store} defaultAmount='' />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Несколько плательщиков, сумма меньше 500', async() => {
            const store = getState();
            const component = <Component store={store} defaultAmount={300} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Выставление счета', () => {
        it('Отрисовка по умолчанию. Юр. лицо', async() => {
            const store = getState();
            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            await page.click('.Radio_type_radio:nth-child(2)');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Отрисовка по умолчанию. Физ. лицо', async() => {
            const store = getState();

            store.billingRequisites = cloneDeep(twoPersonBillingRequisites);
            store.billingRequisites.byId['12001231'].personType = 'ph';
            store.billingRequisites.byId['12001231'].lname = 'Булгаков';
            store.billingRequisites.byId['12001231'].fname = 'Михаил';

            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            await page.click('.Radio_type_radio:nth-child(2)');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('В процессе загрузки счета', async() => {
            const store = getState();
            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            await page.click('.Radio_type_radio:nth-child(2)');
            await page.click(`.${actionButtonStyles.submit}`);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Cумма меньше 500', async() => {
            const store = getState();
            const component = <Component store={store} defaultAmount={300} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            await page.click('.Radio_type_radio:nth-child(2)');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Кредит', () => {
        it('Отрисовка по умолчанию', async() => {
            const store = getState();
            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            await page.click('.Radio_type_radio:nth-child(3)');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Cумма меньше 1000', async() => {
            const store = getState();
            const component = <Component store={store} defaultAmount={300} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            await page.click('.Radio_type_radio:nth-child(3)');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Кредит свыше лимита', async() => {
            const store = getState();

            store.billingRequisites = cloneDeep(singlePersonBillingRequisites);
            store.billingRequisites.byId['12001231'].credit = {
                ...store.billingRequisites.byId['12001231'].credit,
                spent: 34500,
                remain: 300,
                unpaid: 0
            };

            const creditText = JSON.stringify(store.billingRequisites.byId['12001231'].credit, null, 2);

            allure.descriptionHtml(`<pre>${creditText}</pre>`);

            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            await page.click('.Radio_type_radio:nth-child(3)');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Овердрафт', async() => {
            const store = getState();

            store.billingRequisites = cloneDeep(singlePersonBillingRequisites);
            store.billingRequisites.byId['12001231'].credit = {
                ...store.billingRequisites.byId['12001231'].credit,
                spent: 34500,
                remain: 3000,
                unpaid: 80000
            };

            const creditText = JSON.stringify(store.billingRequisites.byId['12001231'].credit, null, 2);

            allure.descriptionHtml(`<pre>${creditText}</pre>`);

            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            await page.click('.Radio_type_radio:nth-child(3)');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Не введена сумма', async() => {
            const store = getState();
            const component = <Component store={store} defaultAmount='' />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            await page.click('.Radio_type_radio:nth-child(3)');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Плательщик без кредита', async() => {
            const store = getState();

            store.billingRequisites = cloneDeep(twoPersonBillingRequisites);
            store.billingRequisites.byId['12001231'].credit = undefined;

            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            await page.click('.Radio_type_radio:nth-child(3)');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Кредит не доступен', async() => {
            const store = getState();

            store.billingRequisites = cloneDeep(singlePersonBillingRequisites);
            store.billingRequisites.byId['12001231'].credit = undefined;

            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Кредит подсказка', async() => {
            const store = getState();

            store.billingRequisites = cloneDeep(singlePersonBillingRequisites);
            store.billingRequisites.byId['12001231'].credit = {
                ...store.billingRequisites.byId['12001231'].credit,
                spent: 34500,
                remain: 1500,
                unpaid: 0
            };

            const creditText = JSON.stringify(store.billingRequisites.byId['12001231'].credit, null, 2);

            allure.descriptionHtml(`<pre>${creditText}</pre>`);

            const component = <Component store={store} />;

            await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

            await page.click(`.${styles.account}`);
            await page.click('.Menu__item:nth-child(2)');

            await page.click('.Radio_type_radio:nth-child(3)');

            await page.hover(`.${creditHintStyles.tip}`);
            await page.waitFor(300);

            expect(await takeScreenshot({
                keepCursor: true
            })).toMatchImageSnapshot();
        });
    });
});
