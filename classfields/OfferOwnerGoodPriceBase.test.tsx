jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});
jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen');

import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';
import type { Action } from 'redux';

import { Currency } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';
import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import paymentModalOpen from 'auto-core/react/dataDomain/state/actions/paymentModalOpen';
import gateApi from 'auto-core/react/lib/gateApi';
import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';
import { TBillingFrom } from 'auto-core/types/TBilling';

import OfferOwnerGoodPriceBase from './OfferOwnerGoodPriceBase';
import type { OwnProps, BaseState, BaseProps } from './OfferOwnerGoodPriceBase';

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

const paymentModalOpenMock = paymentModalOpen as jest.MockedFunction<typeof paymentModalOpen>;
paymentModalOpenMock.mockReturnValue((() => () => { }) as unknown as Action<any>);

class ComponentMock extends OfferOwnerGoodPriceBase<BaseProps, BaseState> {
    render() {
        if (!this.isVisible()) {
            return null;
        }

        return (
            <div>anchor</div>
        );
    }

    isMobile = false

    paymentModalOpen = paymentModalOpenMock
}

let props: OwnProps;

beforeEach(() => {
    props = {
        offer: cardMock,
        from: TBillingFrom.DESKTOP_CARD_GOOD_PRICE_POPUP,
        isCardPage: false,
    };

    jest.useFakeTimers();
});

describe('запрос данных', () => {
    it('произойдет при маунте', () => {
        props.offer = cloneOfferWithHelpers(cardMock)
            .withVehicleInfo({ equipment: { 'mirrors-heat': true } })
            .withMilage(42)
            .value();
        shallowRenderComponent({ props });

        expect(getResource).toHaveBeenCalledTimes(1);
        expect(getResource.mock.calls[0]).toMatchSnapshot();
    });

    describe('не произойдет если', () => {
        it('есть бейдж хорошая цена', () => {
            props.offer = cloneOfferWithHelpers(cardMock).withTags([ 'good_price' ]).value();
            shallowRenderComponent({ props });

            expect(getResource).toHaveBeenCalledTimes(0);
        });

        it('есть бейдж отличная цена', () => {
            props.offer = cloneOfferWithHelpers(cardMock).withTags([ 'excellent_price' ]).value();
            shallowRenderComponent({ props });

            expect(getResource).toHaveBeenCalledTimes(0);
        });

        it('объявление неактивно', () => {
            props.offer = cloneOfferWithHelpers(cardMock).withStatus(OfferStatus.INACTIVE).value();
            shallowRenderComponent({ props });

            expect(getResource).toHaveBeenCalledTimes(0);
        });

        it('подключено поднятие', () => {
            props.offer = cloneOfferWithHelpers(cardMock).withActiveVas([ TOfferVas.FRESH ]).value();
            shallowRenderComponent({ props });

            expect(getResource).toHaveBeenCalledTimes(0);
        });

        it('подключено автоподнятие', () => {
            props.offer = cloneOfferWithHelpers(cardMock).withServiceSchedule(TOfferVas.FRESH).value();
            shallowRenderComponent({ props });

            expect(getResource).toHaveBeenCalledTimes(0);
        });

        it('у оффера нет тех парам айди', () => {
            props.offer = cloneOfferWithHelpers(cardMock).withVehicleInfo({ tech_param: undefined }).value();
            shallowRenderComponent({ props });

            expect(getResource).toHaveBeenCalledTimes(0);
        });

        it('prop shouldFetchOnMount=false', () => {
            props.offer = cloneOfferWithHelpers(cardMock)
                .withVehicleInfo({ equipment: { 'mirrors-heat': true } })
                .withMilage(42)
                .value();
            props.shouldFetchOnMount = false;
            shallowRenderComponent({ props });

            expect(getResource).toHaveBeenCalledTimes(0);
        });
    });
});

describe('показ', () => {
    it('нарисует компонент, если цена выше хорошей', () => {
        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                expect(page.isEmptyRender()).toBe(false);
            });
    });

    it('не нарисует компонент, если цена ниже хорошей', () => {
        props.offer = cloneOfferWithHelpers(cardMock).withPrice(650000).value();
        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                expect(page.isEmptyRender()).toBe(true);
            });
    });

    it('не нарисует компонент, если цена ниже средней', () => {
        props.offer = cloneOfferWithHelpers(cardMock).withPrice(650000).value();
        const getStatsPredictPromise = Promise.resolve({
            prices: {
                market: { currency: Currency.RUR, price: 700000 },
            },
            tag_ranges: {
                good: { from: 580000, to: 600000, currency: Currency.RUR },
                show_tag: true,
            },
            status: 'SUCCESS',
        });
        getResource.mockImplementation(() => getStatsPredictPromise);

        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                expect(page.isEmptyRender()).toBe(true);
            });
    });

    it('не нарисует компонент, если мы ничего не знаем про хорошую цену', () => {
        const getStatsPredictPromise = Promise.resolve({
            prices: {
                market: { currency: Currency.RUR, price: 662000 },
            },
            tag_ranges: {
                show_tag: true,
            },
            status: 'SUCCESS',
        });
        getResource.mockImplementation(() => getStatsPredictPromise);

        const page = shallowRenderComponent({ props });
        return getStatsPredictPromise
            .then(() => {
                expect(page.isEmptyRender()).toBe(true);
            });
    });
});

function shallowRenderComponent({ props }: { props: OwnProps }) {
    const store = mockStore({});
    const ConnectedComponentMock = ComponentMock.connector(ComponentMock);

    const page = shallow(
        <Provider store={ store }>
            <ConnectedComponentMock { ...props }/>
        </Provider>
        ,
        { context: contextMock },
    ).dive().dive();

    return page;
}
