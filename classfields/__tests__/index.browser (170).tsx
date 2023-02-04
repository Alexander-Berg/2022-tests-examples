/* eslint-disable @typescript-eslint/ban-ts-comment */
import React from 'react';
import { render } from 'jest-puppeteer-react';
import cloneDeep from 'lodash/cloneDeep';
import merge from 'lodash/merge';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IAppProviderProps } from 'realty-core/view/react/libs/test-helpers';

import { AppProvider } from 'view/lib/test-helpers';

import TrapPanel from '../container';

import { products, productsWithDiscount } from './mocks';

advanceTo(new Date('2020-10-01T04:17:00.111Z'));

const offerForm = {
    category: 'APARTMENT',
    location: {
        isKnown: true,
        rgid: 0,
    },
    photo: ['url', 'url', 'url', 'url'],
    _result: {
        id: '111',
        placement: {
            paymentRequired: {
                paid: true,
            },
        },
        productInfo: { products, availableMoney: 500 },
    },
};

const store = {
    config: {
        timeDelta: 0,
    },
    offerForm,
    user: {
        isJuridical: false,
    },
    discountInfo: {},
};

const Component = (props: Pick<IAppProviderProps, 'initialState'>) => {
    const { initialState } = props;

    return (
        <AppProvider initialState={initialState}>
            <TrapPanel {...props} />
        </AppProvider>
    );
};

describe('TrapPanel', () => {
    it('Активация с фото, нет активного турбо, есть однодневная скидка, осталось меньше суток', async () => {
        const state = cloneDeep(store);

        state.discountInfo = {
            placement: {
                endDate: '2020-10-01T12:20:00.111Z',
                percent: 90,
            },
        };

        state.offerForm._result.placement = {
            paymentRequired: {
                paid: false,
            },
        };

        const { priceContext } = state.offerForm._result.productInfo.products.placement;

        priceContext.base = 149;
        priceContext.effective = Math.round(0.75 * 149);
        priceContext.modifiers = {
            money: 5,
            bonusDiscount: {
                percent: 75,
                amount: 112,
            },
        };

        const component = <Component initialState={state} />;

        await render(component, { viewport: { width: 1000, height: 900 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Активация с фото, нет активного турбо, есть однодневная скидка, осталось больше суток', async () => {
        const state = cloneDeep(store);

        state.discountInfo = {
            placement: {
                endDate: '2020-10-03T15:20:00.111Z',
                percent: 90,
            },
        };

        state.offerForm._result.placement = {
            paymentRequired: {
                paid: false,
            },
        };

        const { priceContext } = state.offerForm._result.productInfo.products.placement;

        priceContext.base = 149;
        priceContext.effective = Math.round(0.75 * 149);
        // @ts-ignore
        priceContext.modifiers = {
            money: 5,
            bonusDiscount: {
                percent: 75,
                amount: 112,
            },
        };

        const component = <Component initialState={state} />;

        await render(component, { viewport: { width: 1000, height: 900 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Активация с фото, нет активного турбо, есть долгосрочная скидка, осталось меньше суток', async () => {
        const state = cloneDeep(store);

        state.discountInfo = {
            placement: {
                endDate: '2020-10-01T12:20:00.111Z',
                amount: 1,
            },
        };

        state.offerForm._result.placement = {
            paymentRequired: {
                paid: false,
            },
        };

        const { priceContext } = state.offerForm._result.productInfo.products.placement;

        priceContext.base = 149;
        priceContext.effective = Math.round(0.75 * 149);
        priceContext.modifiers = {
            money: 5,
            bonusDiscount: {
                percent: 75,
                amount: 112,
            },
        };

        const component = <Component initialState={state} />;

        await render(component, { viewport: { width: 1000, height: 900 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Активация с фото, нет активного турбо, есть долгосрочная скидка, осталось больше суток', async () => {
        const state = cloneDeep(store);

        state.discountInfo = {
            placement: {
                endDate: '2020-10-03T15:20:00.111Z',
                amount: 1,
            },
        };

        state.offerForm._result.placement = {
            paymentRequired: {
                paid: false,
            },
        };

        const { priceContext } = state.offerForm._result.productInfo.products.placement;

        priceContext.base = 149;
        priceContext.effective = Math.round(0.75 * 149);
        // @ts-ignore
        priceContext.modifiers = {
            money: 5,
            bonusDiscount: {
                percent: 75,
                amount: 112,
            },
        };

        const component = <Component initialState={state} />;

        await render(component, { viewport: { width: 1000, height: 900 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Активация с фото, нет активного турбо, есть однодневная скидка, дата окончания меньше текущей', async () => {
        const state = cloneDeep(store);

        state.discountInfo = {
            placement: {
                endDate: '2020-09-30T15:20:00.111Z',
                percent: 90,
            },
        };

        state.offerForm._result.placement = {
            paymentRequired: {
                paid: false,
            },
        };

        const { priceContext } = state.offerForm._result.productInfo.products.placement;

        priceContext.base = 149;
        priceContext.effective = Math.round(0.75 * 149);
        // @ts-ignore
        priceContext.modifiers = {
            money: 5,
            bonusDiscount: {
                percent: 75,
                amount: 112,
            },
        };

        const component = <Component initialState={state} />;

        await render(component, { viewport: { width: 1000, height: 900 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Публикация с фото, нет активного турбо, есть однодневная скидка, бесплатный оффер', async () => {
        const state = cloneDeep(store);

        state.discountInfo = {
            placement: {
                endDate: '2020-10-1T15:20:00.111Z',
                percent: 90,
            },
        };

        state.offerForm._result.placement = {
            // @ts-ignore
            free: {},
        };

        const component = <Component initialState={state} />;

        await render(component, { viewport: { width: 1000, height: 900 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Публикация с фото, нет активного турбо, есть однодневная скидка, квота', async () => {
        const state = cloneDeep(store);

        state.discountInfo = {
            placement: {
                endDate: '2020-10-1T15:20:00.111Z',
                percent: 90,
            },
        };

        state.offerForm._result.placement = {
            // @ts-ignore
            quota: {},
        };

        const component = <Component initialState={state} />;

        await render(component, { viewport: { width: 1000, height: 900 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Активация с фото, с активным турбо', async () => {
        const state = cloneDeep(store);

        state.offerForm._result.placement = {
            paymentRequired: {
                paid: false,
            },
        };
        state.offerForm._result.productInfo.products.turboSale = merge(
            state.offerForm._result.productInfo.products.turboSale,
            {
                status: 'inactive',
                priceContext: {
                    isAvailable: true,
                    effective: 699,
                    base: 799,
                    reasons: [],
                    modifiers: {
                        money: 100,
                    },
                },
            }
        );

        const component = <Component initialState={state} />;

        await render(component, { viewport: { width: 1000, height: 900 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Публикация с фото', async () => {
        const state = cloneDeep(store);

        state.offerForm._result.placement = {
            paymentRequired: {
                paid: true,
            },
        };

        const component = <Component initialState={state} />;

        await render(component, { viewport: { width: 1000, height: 900 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Активация с фото, нет активного турбо, есть однодневные скидки на VAS', async () => {
        const state = cloneDeep(store);

        // @ts-ignore
        state.offerForm._result.productInfo.products = cloneDeep(productsWithDiscount);
        state.offerForm._result.productInfo.products.turboSale.status = 'active';

        const component = <Component initialState={state} />;

        await render(component, { viewport: { width: 1000, height: 900 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Активация с фото, с активным турбо, есть однодневная скидка на Турбо', async () => {
        const state = cloneDeep(store);

        // @ts-ignore
        state.offerForm._result.productInfo.products = cloneDeep(productsWithDiscount);
        state.offerForm._result.productInfo.products.turboSale.status = 'inactive';

        const component = <Component initialState={state} />;

        await render(component, { viewport: { width: 1000, height: 900 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
