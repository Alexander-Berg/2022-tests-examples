import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';
import _ from 'lodash';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import offerMock from 'autoru-frontend/mockData/state/groupCard.mock';
import stateSupportMock from 'autoru-frontend/mockData/bunker/desktop/state_support.json';

jest.mock('auto-core/react/dataDomain/relatedGroups/actions/fetchMoreRelatedGroups', () => {
    return jest.fn(() => () => {});
});

import type { TGroupInfo } from 'auto-core/react/dataDomain/cardGroup/types';
import equipmentDictionaryMock from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';
import ButtonWithLoader from 'auto-core/react/components/islands/ButtonWithLoader/ButtonWithLoader';
import fetchMoreRelatedGroups from 'auto-core/react/dataDomain/relatedGroups/actions/fetchMoreRelatedGroups';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';
import type { TSearchParameters } from 'auto-core/types/TSearchParameters';

import EmptyGroupRelated from './EmptyGroupRelated';

const params = {
    category: 'cars',
    section: 'new',
    catalog_filter: [
        {
            mark: 'BMW',
        },
    ],
};

const state = {
    geo: {
        radius: 200,
        gidsInfo: [],
    },
    tradein: { offers: [] },
    user: { data: {} },
};

const props = {
    pageParams: params,
    groupingId: '12345',
    groupInfo: {
        mark: {
            name: 'BMW',
        },
    } as TGroupInfo,
    equipmentDictionary: equipmentDictionaryMock.data,
    presentEquipment: [],
    stateSupportData: stateSupportMock,
    searchID: '1234',
    searchParameters: params as TSearchParameters,
    isRelatedPending: false,

    sendMarketingEventByListingOffer: _.noop,
    sellerPopupOpen: _.noop,
    fetchRelatedGroups: fetchMoreRelatedGroups,
};

const getRelatedGroups = (offer: Offer, offersCount: number) => {
    const result = [];

    for (let i = 0; i < offersCount; i++) {
        result.push(offer);
    }

    return result;
};

it('должен отрендерить два дилера и кнопку', async() => {
    const relatedGroups = getRelatedGroups(offerMock, 4);

    const wrapper = shallow(
        <Provider store={ mockStore(state) }>
            <EmptyGroupRelated relatedGroups={ relatedGroups } relatedGroupsCount={ 4 } { ...props }/>
        </Provider>,
        { context: contextMock },
    ).dive();

    const dealersListing = wrapper.find('.EmptyGroupRelated__list');
    const dealers = dealersListing.children();
    const moreButton = wrapper.find(ButtonWithLoader);

    expect(dealers).toHaveLength(2);
    expect(moreButton).toHaveLength(1);
});

it('должен отрендерить всех дилеров при клике на кнопку и спрятать саму кнопку', async() => {
    const relatedGroups = getRelatedGroups(offerMock, 6);

    const wrapper = shallow(
        <Provider store={ mockStore(state) }>
            <EmptyGroupRelated relatedGroups={ relatedGroups } relatedGroupsCount={ 6 } { ...props }/>
        </Provider>,
        { context: contextMock },
    ).dive();

    wrapper.find(ButtonWithLoader).simulate('click');

    const dealersListing = wrapper.find('.EmptyGroupRelated__list');
    const dealers = dealersListing.children();

    const moreButton = wrapper.find(ButtonWithLoader);

    expect(dealers).toHaveLength(6);
    expect(moreButton).toHaveLength(0);
    expect(fetchMoreRelatedGroups).not.toHaveBeenCalled();
});

it('должен запросить еще дилеров при клике на кнопку второй раз', async() => {
    const relatedGroups = getRelatedGroups(offerMock, 10);

    const wrapper = shallow(
        <Provider store={ mockStore(state) }>
            <EmptyGroupRelated relatedGroups={ relatedGroups } relatedGroupsCount={ 12 } { ...props }/>
        </Provider>,
        { context: contextMock },
    ).dive();

    const moreButton = wrapper.find(ButtonWithLoader);

    moreButton.simulate('click');

    const dealersListing = wrapper.find('.EmptyGroupRelated__list');
    const dealers = dealersListing.children();

    expect(dealers).toHaveLength(10);

    moreButton.simulate('click');

    expect(fetchMoreRelatedGroups).toHaveBeenCalledTimes(1);
});
