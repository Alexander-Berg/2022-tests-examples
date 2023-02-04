/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen');
jest.mock('auto-core/react/dataDomain/card/actions/updateOfferPrice');
jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';
import type { Action } from 'redux';

import { Currency } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import paymentModalOpen from 'auto-core/react/dataDomain/state/actions/paymentModalOpen';
import updateOfferPrice from 'auto-core/react/dataDomain/card/actions/updateOfferPrice';
import gateApi from 'auto-core/react/lib/gateApi';
import type { OwnProps } from 'auto-core/react/components/common/OfferOwnerGoodPriceBase/OfferOwnerGoodPriceBase';
import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import { TBillingFrom } from 'auto-core/types/TBilling';

import OfferOwnerGoodPriceMobile from './OfferOwnerGoodPriceMobile';

const updatePricePromise = Promise.resolve();
const updateOfferPriceMock = updateOfferPrice as jest.MockedFunction<typeof updateOfferPrice>;
updateOfferPriceMock.mockReturnValue((() => updatePricePromise));

const paymentModalOpenMock = paymentModalOpen as jest.MockedFunction<typeof paymentModalOpen>;
paymentModalOpenMock.mockReturnValue((() => () => {}) as unknown as Action);

const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;
const statsPredictMock = {
    prices: {
        autoru: { currency: Currency.RUR, from: 662000, to: 662000 },
        tradein: { currency: Currency.RUR, from: 662000, to: 662000 },
        market: { currency: Currency.RUR, price: 662000 },
    },
    tag_ranges: {
        excellent: { from: 436050, to: 580000, currency: Currency.RUR },
        good: { from: 580000, to: 698000, currency: Currency.RUR },
        show_tag: true,
    },
    status: 'SUCCESS',
};
const getStatsPredictPromise = Promise.resolve(statsPredictMock);
getResource.mockImplementation(() => getStatsPredictPromise);

let props: OwnProps;

beforeEach(() => {
    props = {
        offer: cardMock,
        from: TBillingFrom.DESKTOP_CARD_GOOD_PRICE_POPUP,
        isCardPage: false,
    };

    jest.useFakeTimers();
});

it('при маунте покажет пустой экран', () => {
    const page = shallowRenderComponent({ props });
    expect(page.prop('screen')).toBeUndefined();

    return getStatsPredictPromise
        .then(() => {
            expect(page.prop('screen')).toBe('');
        });
});

it('при клике по якорю, покажет начальный экран', () => {
    const page = shallowRenderComponent({ props });
    return getStatsPredictPromise
        .then(() => {
            page.simulate('anchorClick');

            expect(page.prop('screen')).toBe('init');
        });
});

it('при клике по подсказке, покажет экран с подсказкой', () => {
    const page = shallowRenderComponent({ props });
    return getStatsPredictPromise
        .then(() => {
            page.simulate('aboutIconClick');

            expect(page.prop('screen')).toBe('about');
        });
});

it('при клике по ссылки "изменить цену", покажет экран с ценой', () => {
    const page = shallowRenderComponent({ props });
    return getStatsPredictPromise
        .then(() => {
            page.simulate('changePriceClick');

            expect(page.prop('screen')).toBe('price');
        });
});

it('при клике по средней цене, покажет экран с ценой', () => {
    const page = shallowRenderComponent({ props });
    return getStatsPredictPromise
        .then(() => {
            page.simulate('avgPriceClick');

            expect(page.prop('price')).toBe(662000);
            expect(page.prop('screen')).toBe('price');
        });
});

it('после изменения цены, покажет начальный экран', () => {
    const page = shallowRenderComponent({ props });
    return getStatsPredictPromise
        .then(() => {
            page.simulate('changePriceClick');
            page.simulate('priceSubmit');

            expect(page.prop('screen')).toBe('price');

            return updatePricePromise
                .then(() => {
                    expect(page.prop('screen')).toBe('init');
                });
        });
});

describe('при клике на крест', () => {
    it('на экране с ценой, вернется на начальный экран', () => {
        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                page.simulate('changePriceClick');
                page.simulate('requestHide', undefined, 'clickModalCloser');

                expect(page.prop('screen')).toBe('init');
            });
    });

    it('на экране с подсказкой, вернется на начальный экран', () => {
        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                page.simulate('aboutIconClick');
                page.simulate('requestHide', undefined, 'clickModalCloser');

                expect(page.prop('screen')).toBe('init');
            });
    });

    it('на начальном экране, закроет модал', () => {
        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                page.simulate('anchorClick');
                page.simulate('requestHide', undefined, 'clickModalCloser');

                expect(page.prop('screen')).toBe('');
            });
    });
});

describe('при клике вне модала', () => {
    it('на экране с ценой, закроет модал', () => {
        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                page.simulate('changePriceClick');
                page.simulate('requestHide', undefined, 'clickOutside');

                expect(page.prop('screen')).toBe('');
            });
    });

    it('на начальном экране, закроет модал', () => {
        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                page.simulate('anchorClick');
                page.simulate('requestHide', undefined, 'clickOutside');

                expect(page.prop('screen')).toBe('');
            });
    });
});

describe('при клике на кнопку купить', () => {
    beforeEach(() => {
        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                page.simulate('buyButtonClick');
            });
    });

    it('откроет модал оплаты', () => {
        expect(paymentModalOpenMock).toHaveBeenCalledTimes(1);
        expect(paymentModalOpenMock.mock.calls[0]).toMatchSnapshot();
    });

    it('отправит вас лог', () => {
        expect(contextMock.logVasEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.logVasEvent.mock.calls[0]).toMatchSnapshot();
    });
});

function shallowRenderComponent({ props }: { props: OwnProps }) {
    const store = mockStore({});

    const page = shallow(
        <Provider store={ store }>
            <OfferOwnerGoodPriceMobile { ...props }/>
        </Provider>
        ,
        { context: contextMock },
    ).dive().dive();

    return page;
}
