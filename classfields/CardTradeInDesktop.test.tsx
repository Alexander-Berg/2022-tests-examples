/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

jest.mock('auto-core/react/dataDomain/tradein/actions/submitForm');
// eslint-disable-next-line @typescript-eslint/no-use-before-define
const submitFormMock = submitForm as jest.MockedFunction<typeof submitForm>;
submitFormMock.mockImplementation(() => () => Promise.resolve() as any);

jest.mock('auto-core/lib/event-log/statApi');
jest.mock('auto-core/lib/sendMetrikaForContact');

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import flushPromises from 'autoru-frontend/jest/unit/flushPromises';

import sendMetrikaForContact from 'auto-core/lib/sendMetrikaForContact';
import statApi from 'auto-core/lib/event-log/statApi';

import userMock from 'auto-core/react/dataDomain/user/mocks';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import type { ReduxState } from 'auto-core/react/components/common/TradeinAbstract/TradeinAbstract';
import submitForm from 'auto-core/react/dataDomain/tradein/actions/submitForm';
import getOfferPrice from 'auto-core/react/lib/offer/dataSets/tradein/getOfferPrice';
import getPriceInfo from 'auto-core/react/lib/offer/getPrice';

import CardTradeInDesktop from './CardTradeInDesktop';
import type { OwnProps } from './CardTradeInDesktop';

let props: OwnProps;
let initialState: ReduxState;

beforeEach(() => {
    props = {
        offer: cloneOfferWithHelpers(offerMock).withRedemptionAvailable(true).value(),
    };
    initialState = {
        tradein: {
            offers: [ cloneOfferWithHelpers(offerMock).withPrice(600000).value() ],
            tradeinPrice: {
                data: 571000,
                error: false,
                pending: false,
            },
        },
        card: props.offer,
        user: userMock.withAuth(true).value(),
        searchID: {
            searchID: 'searchID',
            parentSearchId: 'parentSearchID',
        },
    };
});

describe('показ компонента', () => {
    it('показывается, если все условия соблюдены', () => {
        const page = shallowRenderComponent({ props, initialState });

        expect(page.isEmptyRender()).toBe(false);
    });

    describe('не показывается', () => {
        it('если оффер дилера не доступен для трейд-ина', () => {
            props.offer = cloneOfferWithHelpers(offerMock).withRedemptionAvailable(false).value();
            const page = shallowRenderComponent({ props, initialState });

            expect(page.isEmptyRender()).toBe(true);
        });

        it('если у пользователя нет офферов', () => {
            initialState.tradein.offers = [];
            const page = shallowRenderComponent({ props, initialState });

            expect(page.isEmptyRender()).toBe(true);
        });

        it('если нет инфы про трейд-ин от ручки', () => {
            initialState.tradein.tradeinPrice.data = null;
            const page = shallowRenderComponent({ props, initialState });

            expect(page.isEmptyRender()).toBe(true);
        });

        it('если разница цен офферов слишком маленькая', () => {
            const price = getOfferPrice(props.offer) - 30000;
            initialState.tradein.tradeinPrice.data = price;
            const page = shallowRenderComponent({ props, initialState });

            expect(page.isEmptyRender()).toBe(true);
        });
    });
});

describe('цена за трейд-ин', () => {
    it('берется из ручки, если запрос в стату был успешный', () => {
        const page = shallowRenderComponent({ props, initialState });
        const userOfferPriceBlock = page.find('.CardTradeInDesktop__userOfferPrice');

        expect(userOfferPriceBlock.prop('price')).toBe(initialState.tradein.tradeinPrice.data);
    });

    it('будет 90% от цены оффера, если запрос в стату упал', () => {
        initialState.tradein.tradeinPrice.error = true;
        const expectedPrice = getPriceInfo(initialState.tradein.offers[0]).price * 0.9;
        const page = shallowRenderComponent({ props, initialState });
        const userOfferPriceBlock = page.find('.CardTradeInDesktop__userOfferPrice');

        expect(userOfferPriceBlock.prop('price')).toBe(expectedPrice);
    });
});

it('при маунте отправит метрику', () => {
    shallowRenderComponent({ props, initialState });

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'tradein-new-block', 'show' ]);
});

describe('при сабмите формы', () => {
    it('отправит гол в метрику', () => {
        const page = shallowRenderComponent({ props, initialState });
        const form = page.find('Connect(TradeinForm)');
        form.simulate('submit', { foo: 'bar' });

        expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('TRADEIN_NEW_CL_CARS2');
    });

    it('отправит метрику контакта дилера', () => {
        const page = shallowRenderComponent({ props, initialState });
        const form = page.find('Connect(TradeinForm)');
        form.simulate('submit', { foo: 'bar' });

        expect(sendMetrikaForContact).toHaveBeenCalledTimes(1);
        expect(sendMetrikaForContact).toHaveBeenCalledWith(props.offer, contextMock.metrika, false);
    });

    it('при успешном запросе отправит событие страницы и лог', async() => {
        const page = shallowRenderComponent({ props, initialState });
        const form = page.find('Connect(TradeinForm)');
        form.simulate('submit', { foo: 'bar' });

        await flushPromises();

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenLastCalledWith([ 'tradein-new-block', 'send' ]);

        expect(statApi.logImmediately).toHaveBeenCalledTimes(1);
        expect(statApi.logImmediately).toHaveBeenCalledWith({
            trade_in_request_send_event: {
                card_id: '1085562758-1970f439',
                category: 'CARS',
                context_page: 'PAGE_CARD',
                group_size: 0,
                grouping_id: '',
                index: 0,
                page_type: 'CARD',
                search_position: 0,
                section: 'USED',
                self_type: 'TYPE_SINGLE',
                phone_number: '',
                name: '',
                search_query_id: 'searchID',
            },
        });
    });

    it('при не успешном запросе отправит событие страницы', async() => {
        submitFormMock.mockImplementationOnce(() => () => Promise.reject() as any);
        const page = shallowRenderComponent({ props, initialState });
        const form = page.find('Connect(TradeinForm)');
        form.simulate('submit', { foo: 'bar' });

        await flushPromises();

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenLastCalledWith([ 'tradein-new-block', 'err' ]);
    });
});

function shallowRenderComponent({ initialState, props }: { props: OwnProps; initialState: ReduxState }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    const page = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <CardTradeInDesktop { ...props }/>
            </Provider>
        </ContextProvider>,
    );

    return page.dive().dive().dive();
}
