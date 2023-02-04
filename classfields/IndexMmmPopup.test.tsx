jest.mock('auto-core/react/dataDomain/breadcrumbsPublicApi/actions/fetchWithFilters', () => {
    return jest.fn(() => ({ type: '_action_fetchWithFilters' }));
});
jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(() => Promise.reject('JEST_MOCK_REJECT')),
    };
});

import { noop } from 'lodash';
import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';

import fetchBreadcrumbs from 'auto-core/react/dataDomain/breadcrumbsPublicApi/actions/fetchWithFilters';
import gateApi from 'auto-core/react/lib/gateApi';

import IndexMmmPopup from './IndexMmmPopup';

const Context = createContextProvider(contextMock);

let state: Record<string, unknown>;
beforeEach(() => {
    state = {};
});

describe('Изменение МММ', () => {
    it('не должен запросить крошки и счетчики, если источник изменения - закрытия попапа', () => {
        const wrapper = shallow(
            <Context>
                <Provider store={ mockStore(state) }>
                    <IndexMmmPopup
                        pageParams={{}}
                        onRequestHide={ noop }
                        onSubmit={ noop }
                        visible={ true }
                    />
                </Provider>
            </Context>,
        ).dive().dive().dive();

        wrapper.find('ListingMmmPopup').simulate('change', [], { fetchBreadcrumbs: true, source: 'onRequestHide' });

        expect(fetchBreadcrumbs).not.toHaveBeenCalled();
        expect(gateApi.getResource).not.toHaveBeenCalled();
    });

    it('должен запросить крошки и счетчики после изменения, если есть флаг fetchBreadcrumbs=true', () => {
        const wrapper = shallow(
            <Context>
                <Provider store={ mockStore(state) }>
                    <IndexMmmPopup
                        pageParams={{}}
                        onRequestHide={ noop }
                        onSubmit={ noop }
                        visible={ true }
                    />
                </Provider>
            </Context>,
        ).dive().dive().dive();

        wrapper.find('ListingMmmPopup').simulate('change', [], { fetchBreadcrumbs: true, source: 'change' });

        expect(fetchBreadcrumbs).toHaveBeenCalledTimes(1);
        expect(gateApi.getResource).toHaveBeenCalledTimes(1);
        expect(gateApi.getResource).toHaveBeenCalledWith('listingCount', { catalog_filter: [], exclude_catalog_filter: [] });
    });

    it('должен запросить только счетчики после изменения, если есть флаг fetchBreadcrumbs=false', () => {
        const wrapper = shallow(
            <Context>
                <Provider store={ mockStore(state) }>
                    <IndexMmmPopup
                        pageParams={{}}
                        onRequestHide={ noop }
                        onSubmit={ noop }
                        visible={ true }
                    />
                </Provider>
            </Context>,
        ).dive().dive().dive();

        wrapper.find('ListingMmmPopup').simulate('change', [], { fetchBreadcrumbs: false, source: 'change' });

        expect(fetchBreadcrumbs).toHaveBeenCalledTimes(0);
        expect(gateApi.getResource).toHaveBeenCalledTimes(1);
        expect(gateApi.getResource).toHaveBeenCalledWith('listingCount', { catalog_filter: [], exclude_catalog_filter: [] });
    });
});
