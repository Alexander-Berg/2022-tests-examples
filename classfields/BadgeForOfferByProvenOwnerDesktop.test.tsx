import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import Badge from 'auto-core/react/components/common/Badges/Badge/Badge';

import BadgeForOfferByProvenOwnerDesktop from './BadgeForOfferByProvenOwnerDesktop';

const Context = createContextProvider(contextMock);
const store = mockStore({});

it('должен нарисовать бейдж для оффера от собственника', async() => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withTags([ 'proven_owner' ])
        .value();

    const tree = shallow(
        <Context>
            <Provider store={ store }>
                <BadgeForOfferByProvenOwnerDesktop offer={ offer } color={ Badge.COLOR.BLUE_GRAY_LIGHT_EXTRA }/>
            </Provider>
        </Context>,
    ).dive().dive().dive();

    expect(tree.find('Badge')).toExist();
});

it('не должен нарисовать бейдж для оффера, если собственник не проверен', async() => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withTags([])
        .value();

    const tree = shallow(
        <Context>
            <Provider store={ store }>
                <BadgeForOfferByProvenOwnerDesktop offer={ offer } color={ Badge.COLOR.BLUE_GRAY_LIGHT_EXTRA }/>
            </Provider>
        </Context>,
    ).dive().dive().dive();

    expect(tree).toBeEmptyRender();
});

it('не должен нарисовать бейдж для оффера, если авто забронировано', async() => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withBookingStatus('BOOKED')
        .value();

    const tree = shallow(
        <Context>
            <Provider store={ store }>
                <BadgeForOfferByProvenOwnerDesktop offer={ offer } color={ Badge.COLOR.BLUE_GRAY_LIGHT_EXTRA }/>
            </Provider>
        </Context>,
    ).dive().dive().dive();

    expect(tree).toBeEmptyRender();
});
