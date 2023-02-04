import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';
import { createMemoryHistory } from 'history';
import _ from 'lodash';

import contextMock from 'autoru-frontend/mocks/contextMock';
import routerMock from 'autoru-frontend/mocks/routerMock';

import ROUTES from 'auto-core/router/cabinet.auto.ru/route-names.json';

import CallsListingHeader from 'www-cabinet/react/components/CallsListingHeader/CallsListingHeader';
import CallsListing from 'www-cabinet/react/components/CallsListing/CallsListing';
import calls from 'www-cabinet/react/dataDomain/calls/mocks/withCalls.mock';
import settingsMock from 'www-cabinet/react/dataDomain/calls/mocks/withSettings.mock';
import type { EnrichedCall } from 'www-cabinet/react/dataDomain/calls/types';

import CallsDumb from './CallsDumb';

const baseProps = {
    callsList: calls.callsList.calls as unknown as Array<EnrichedCall>,
    dealerId: 12345,
    pagination: {},
    isPageLoading: false,
    callsPlayer: {},
    callsTotalStats: {},
    callsDailyStats: [],
    callsSettings: settingsMock,
    filters: {},
    breadcrumbs: [],
    isAvailableOfferFilters: true,
    hasCallTariff: true,

    requestExport: () => Promise.resolve(),
    onShowMore: _.noop,
    sendComplaint: () => Promise.resolve(),
    sendRedirectComplaint: jest.fn().mockResolvedValue(undefined),
    downloadRecord: _.noop,
    addTag: () => Promise.resolve(),
    removeTag: () => Promise.resolve(),
    getTags: () => Promise.resolve([]),
    playCallRecord: _.noop,
    pauseCallRecord: _.noop,
    hidePlayer: _.noop,
    getTranscription: () => Promise.resolve('SUCCESS'),

    history: createMemoryHistory(),
    location: { pathname: '', search: '', state: '', hash: '' },
    match: { params: { id: 1 }, isExact: true, path: '', url: '' },

    router: routerMock,
    routeName: ROUTES.calls,
    routeParams: { foo: 'bar' },
};

it('должен рендерить листинг с хэдером, если есть звонки', () => {
    const tree = shallow(
        <CallsDumb { ...baseProps }/>,
        { context: contextMock },
    );

    const listing = tree.find(CallsListing);
    const listingHeader = tree.find(CallsListingHeader);

    expect(shallowToJson(listing)).not.toBeNull();
    expect(shallowToJson(listingHeader)).not.toBeNull();
});

it('не должен рендерить листинг с хэдером, если нет звонков', () => {
    const tree = shallow(
        <CallsDumb { ...baseProps } callsList={ [] }/>,
        { context: contextMock },
    );

    const listing = tree.find(CallsListing);
    const listingHeader = tree.find(CallsListingHeader);

    expect(shallowToJson(listing)).toBeNull();
    expect(shallowToJson(listingHeader)).toBeNull();
});
