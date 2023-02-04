import React from 'react';
import { shallow } from 'enzyme';
import MockDate from 'mockdate';
import { Provider } from 'react-redux';
import type { ShallowWrapper } from 'enzyme';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import ResellerSalesItem from './ResellerSalesItem';
import type { Props } from './ResellerSalesItem';

let props: Props;
let defaultOffer: Offer;

beforeEach(() => {
    MockDate.set('2021-01-01');

    defaultOffer = cloneOfferWithHelpers(offerMock)
        .withCreationDate(String(new Date('2021-03-15T12:00:00Z').getTime()))
        .withExpireDate(String(new Date('2021-04-03T12:00:00Z').getTime()))
        .withServices(offerMock.services.map(item => ({
            ...item, expire_date: String(new Date('2021-04-03T12:00:00Z').getTime()),
        })))
        .value();

    props = {
        offer: defaultOffer,
        stats: undefined,
        onAutoProlongationToggle: () => Promise.resolve(),
        onOfferStateChange: () => {},
        onOfferActivate: () => Promise.resolve(),
        onOfferDelete: () => {},
        onOfferHide: () => {},
        onOfferLoadStats: () => Promise.resolve(),
        onOfferLoadMoreStats: () => Promise.resolve(),
        onOfferUpdate: () => Promise.resolve(),
        onPaymentModalOpen: () => {},
        onPriceChange: () => Promise.resolve(),
        hasTiedCards: true,
        onAutoRenewChange: () => {},
    };
});

describe('содержимое выпадашки', () => {
    it('все элементы, если условия выполняются', () => {
        const wrapper = shallowRenderComponent({ props });

        const list = getDropdownChildrenKeys(wrapper);

        expect(list).toEqual([ 'contract', 'print-offer', 'share', 'calls-history' ]);
    });

    it('не рендерит лист продажи и историю звонков, если категория не cars', () => {
        const wrapper = shallowRenderComponent({ props: {
            ...props,
            offer: cloneOfferWithHelpers(defaultOffer).withCategory('moto').value(),
        } });

        const list = getDropdownChildrenKeys(wrapper);

        expect(list).toEqual([ 'contract', 'share' ]);
    });

    it('не рендерит историю звонков, если нет redirect_phones', () => {
        const wrapper = shallowRenderComponent({ props: {
            ...props,
            offer: cloneOfferWithHelpers(defaultOffer).withSeller({ ...defaultOffer.seller, redirect_phones: false }).value(),
        } });

        const list = getDropdownChildrenKeys(wrapper);

        expect(list).toEqual([ 'contract', 'print-offer', 'share' ]);
    });

    it('не рендерит историю звонков, если нет counters.calls_all', () => {
        const wrapper = shallowRenderComponent({ props: {
            ...props,
            offer: cloneOfferWithHelpers(defaultOffer).withCounters({}).value(),
        } });

        const list = getDropdownChildrenKeys(wrapper);

        expect(list).toEqual([ 'contract', 'print-offer', 'share' ]);
    });

    it('не рендерит историю звонков, если counters.calls_all === 0', () => {
        const wrapper = shallowRenderComponent({ props: {
            ...props,
            offer: cloneOfferWithHelpers(defaultOffer).withCounters({ calls_all: 0 }).value(),
        } });

        const list = getDropdownChildrenKeys(wrapper);

        expect(list).toEqual([ 'contract', 'print-offer', 'share' ]);
    });
});

// сравниваем массив key элементов внутри модала, чтобы не писать большой снэпшот
function getDropdownChildrenKeys(wrapper: ShallowWrapper) {
    wrapper.find('.ResellerSalesItem').simulate('mouseEnter');
    wrapper.find('.ResellerSalesItem__controls').find('Dropdown').dive().find('.Dropdown__switcher').simulate('click');
    const menu = wrapper.find('.ResellerSalesItem__moreMenu');

    return menu.children().map(item => item.key());
}

function shallowRenderComponent({ props }: { props: Props }) {
    const ContextProvider = createContextProvider(contextMock);

    const page = shallow(
        <ContextProvider>
            <Provider store={ mockStore() }>
                <ResellerSalesItem { ...props }/>
            </Provider>
        </ContextProvider>,
    );

    return page.dive().dive();
}
