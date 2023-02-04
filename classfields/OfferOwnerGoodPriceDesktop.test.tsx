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
jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');

import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';
import type { Action } from 'redux';

import { Currency } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import paymentModalOpen from 'auto-core/react/dataDomain/state/actions/paymentModalOpen';
import updateOfferPrice from 'auto-core/react/dataDomain/card/actions/updateOfferPrice';
import { showAutoclosableMessage, showAutoclosableErrorMessage, VIEW } from 'auto-core/react/dataDomain/notifier/actions/notifier';
import gateApi from 'auto-core/react/lib/gateApi';
import type { OwnProps } from 'auto-core/react/components/common/OfferOwnerGoodPriceBase/OfferOwnerGoodPriceBase';
import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import { TBillingFrom } from 'auto-core/types/TBilling';

import OfferOwnerGoodPriceDesktop from './OfferOwnerGoodPriceDesktop';

const showAutoclosableMessageMock = showAutoclosableMessage as jest.MockedFunction<typeof showAutoclosableMessage>;
const showAutoclosableErrorMessageMock = showAutoclosableErrorMessage as jest.MockedFunction<typeof showAutoclosableErrorMessage>;

const updatePricePromise = Promise.resolve();
const updateOfferPriceMock = updateOfferPrice as jest.MockedFunction<typeof updateOfferPrice>;
updateOfferPriceMock.mockReturnValue((() => updatePricePromise));

const paymentModalOpenMock = paymentModalOpen as jest.MockedFunction<typeof paymentModalOpen>;
paymentModalOpenMock.mockReturnValue((() => () => { }) as unknown as Action<any>);

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

describe('при изменении значения в инпуте', () => {
    it('меняет значение в стейте', () => {
        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                page.simulate('inputChange', '');
                expect(page.prop('price')).toBe(0);
            });
    });

    it('сбрасывает ошибку', () => {
        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                page.simulate('inputChange', '');
                page.simulate('priceSubmit');

                expect(page.prop('inputError')).toBe('Укажите цену от 1 500 ₽ до 1 000 000 000 ₽');

                page.simulate('inputChange', '1');
                expect(page.prop('inputError')).toBe('');
            });
    });
});

describe('при сабмите новой цены', () => {
    it('если цена не указана покажет ошибку', () => {
        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                page.simulate('inputChange', '42');
                page.simulate('priceSubmit');

                expect(page.prop('inputError')).toBe('Укажите цену от 1 500 ₽ до 1 000 000 000 ₽');
            });
    });

    describe('если цена указана', () => {
        let page: ShallowWrapper;

        beforeEach(() => {
            page = shallowRenderComponent({ props });
            return getStatsPredictPromise
                .then(() => {
                    page.simulate('priceSubmit');
                });
        });

        it('вызовет экшен', () => {
            expect(updateOfferPriceMock).toHaveBeenCalledTimes(1);
            expect(updateOfferPriceMock).toHaveBeenCalledWith({
                category: 'cars', currency: Currency.RUR, offerID: '1085562758-1970f439', price: 855000,
            });
        });

        it('при успехе покажет нотификацию и отправит метрику', () => {
            return updatePricePromise
                .then(() => {
                    expect(showAutoclosableMessageMock).toHaveBeenCalledTimes(1);
                    expect(showAutoclosableMessageMock).toHaveBeenCalledWith({
                        message: 'Цена успешно изменена', view: VIEW.SUCCESS,
                    });

                    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
                    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'high_price', 'change' ]);
                });
        });
    });

    it('если цену сохранить не удалось, покажет сообщение об ошибке и раздизейблит инпут', () => {
        const pr = Promise.reject();
        updateOfferPriceMock.mockReturnValueOnce(() => pr);

        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                page.simulate('priceSubmit');

                return pr
                    .catch(() => { })
                    .then(() => {
                        expect(showAutoclosableErrorMessageMock).toHaveBeenCalledTimes(1);
                        expect(page.prop('isSubmitted')).toBe(false);
                    });
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

describe('при открытии попапа', () => {
    it('отправит вас лог и метрику только при наведении на якорь', () => {
        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                page.simulate('popupOpen', { isTargetPopup: false });

                expect(contextMock.logVasEvent).toHaveBeenCalledTimes(1);
                expect(contextMock.logVasEvent.mock.calls[0]).toMatchSnapshot();

                expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
                expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'high_price', 'hover' ]);
            });
    });

    it('ничего не отправит при наведении на сам попап', () => {
        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                page.simulate('popupOpen', { isTargetPopup: false });
                contextMock.logVasEvent.mockClear();
                contextMock.metrika.sendPageEvent.mockClear();

                page.simulate('popupOpen', { isTargetPopup: true });

                expect(contextMock.logVasEvent).toHaveBeenCalledTimes(0);
                expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
            });
    });
});

it('при фокусе на инпут покажет кнопку "применить"', () => {
    const page = shallowRenderComponent({ props });
    return getStatsPredictPromise
        .then(() => {
            expect(page.prop('showApplyButton')).toBe(false);

            page.simulate('inputFocus');

            expect(page.prop('showApplyButton')).toBe(true);
        });
});

describe('при потере фокуса с инпута', () => {
    it('если я ткнул мимо кнопки "применить", уберет кнопку через таймаут', () => {
        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                page.simulate('inputFocus');
                page.simulate('inputBlur');

                expect(page.prop('showApplyButton')).toBe(true);

                jest.advanceTimersByTime(50);

                expect(page.prop('showApplyButton')).toBe(false);
            });
    });

    it('если я нажал на кнопку "применить" и есть ошибка в инпуте, не уберет кнопку через таймаут', () => {
        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                page.simulate('inputFocus');
                page.simulate('inputChange', '');
                page.simulate('inputBlur');
                page.simulate('priceSubmit');

                jest.advanceTimersByTime(50);

                expect(page.prop('showApplyButton')).toBe(true);
            });
    });

    it('если я нажал на кнопку "применить" и все ок, не уберет кнопку через таймаут', () => {
        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                page.simulate('inputFocus');
                page.simulate('inputBlur');
                page.simulate('priceSubmit');

                jest.advanceTimersByTime(50);

                expect(page.prop('showApplyButton')).toBe(true);
            });
    });
});

it('при клике по средней цене, подставит значение в инпут и покажет кнопку', () => {
    const page = shallowRenderComponent({ props });
    return getStatsPredictPromise
        .then(() => {
            page.simulate('inputBlur');
            page.simulate('avgPriceClick');

            expect(page.prop('price')).toBe(662000);
            expect(page.prop('showApplyButton')).toBe(true);

            jest.advanceTimersByTime(50);

            expect(page.prop('showApplyButton')).toBe(true);
        });
});

function shallowRenderComponent({ props }: { props: OwnProps }) {
    const store = mockStore({});

    const page = shallow(
        <Provider store={ store }>
            <OfferOwnerGoodPriceDesktop { ...props }/>
        </Provider>
        ,
        { context: contextMock },
    ).dive().dive();

    return page;
}
