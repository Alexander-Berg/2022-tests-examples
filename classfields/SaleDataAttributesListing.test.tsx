import { cloneDeep } from 'lodash';
import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';
import 'jest-enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';
import mockListing from 'autoru-frontend/mockData/state/listing';

import mockOfferCars from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import mockOfferMotorcycle from 'auto-core/react/dataDomain/card/mocks/card.motorcycle.mock';
import mockOfferLcv from 'auto-core/react/dataDomain/card/mocks/card.lcv.mock';
import mockOfferTrailer from 'auto-core/react/dataDomain/card/mocks/card.trailer.mock';
import mockOfferTruck from 'auto-core/react/dataDomain/card/mocks/card.truck.mock';

import SaleDataAttributesListing from './SaleDataAttributesListing';

it('должен добавить asciiCat, mark, puid2, puid10, segment, type для CARS', () => {
    const listing = cloneDeep(mockListing);
    listing.data.offers = [ mockOfferCars ];
    listing.data.search_parameters = {
        category: 'cars',
        section: 'used',
    };
    const state = {
        listing: listing,
    };

    const wrapper = shallow(
        <Provider store={ mockStore(state) }>
            <SaleDataAttributesListing/>
        </Provider>,
    ).dive().dive();

    expect(wrapper.find('SaleDataAttributes')).toHaveProp('attributes', {
        asciiCat: 'cars',
        mark: 'FORD',
        segment: 'MEDIUM',
        puid2: '3152',
        puid10: '1',
        type: 'suv',
    });
});

it('должен добавить asciiCat, mark, puid2, puid10 для MOTORCYCLE', () => {
    const listing = cloneDeep(mockListing);
    listing.data.offers = [ mockOfferMotorcycle ];
    listing.data.search_parameters = {
        category: 'moto',
        moto_category: 'MOTORCYCLE',
        section: 'used',
    };
    const state = {
        listing: listing,
    };

    const wrapper = shallow(
        <Provider store={ mockStore(state) }>
            <SaleDataAttributesListing/>
        </Provider>,
    ).dive().dive();

    expect(wrapper.find('SaleDataAttributes')).toHaveProp('attributes', {
        asciiCat: 'moto',
        mark: 'HARLEY_DAVIDSON',
        puid2: '11144251',
        puid10: '3',
    });
});

it('должен добавить asciiCat, mark, puid2, puid10 для LCV', () => {
    const listing = cloneDeep(mockListing);
    listing.data.offers = [ mockOfferLcv ];
    listing.data.search_parameters = {
        category: 'trucks',
        trucks_category: 'LCV',
        section: 'used',
    };
    const state = {
        listing: listing,
    };

    const wrapper = shallow(
        <Provider store={ mockStore(state) }>
            <SaleDataAttributesListing/>
        </Provider>,
    ).dive().dive();

    expect(wrapper.find('SaleDataAttributes')).toHaveProp('attributes', {
        asciiCat: 'trucks',
        mark: 'GAZ',
        puid2: '11203632',
        puid10: '2',
    });
});

it('должен добавить asciiCat, mark, puid2, puid10 для TRUCK', () => {
    const listing = cloneDeep(mockListing);
    listing.data.offers = [ mockOfferTruck ];
    listing.data.search_parameters = {
        category: 'trucks',
        trucks_category: 'TRUCK',
        section: 'used',
    };
    const state = {
        listing: listing,
    };

    const wrapper = shallow(
        <Provider store={ mockStore(state) }>
            <SaleDataAttributesListing/>
        </Provider>,
    ).dive().dive();

    expect(wrapper.find('SaleDataAttributes')).toHaveProp('attributes', {
        asciiCat: 'trucks',
        mark: 'KAMAZ',
        puid2: '11203636',
        puid10: '2',
    });
});

it('должен добавить asciiCat, puid10, type для TRAILER', () => {
    const listing = cloneDeep(mockListing);
    listing.data.offers = [ mockOfferTrailer ];
    listing.data.search_parameters = {
        category: 'trucks',
        trucks_category: 'TRAILER',
        section: 'used',
    };
    const state = {
        listing: listing,
    };

    const wrapper = shallow(
        <Provider store={ mockStore(state) }>
            <SaleDataAttributesListing/>
        </Provider>,
    ).dive().dive();

    expect(wrapper.find('SaleDataAttributes')).toHaveProp('attributes', {
        asciiCat: 'trucks',
        puid10: '2',
        type: 'trailer',
    });
});
