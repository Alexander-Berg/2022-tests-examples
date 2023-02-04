jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
    };
});

import React from 'react';
import { shallow } from 'enzyme';
import { Provider, useSelector } from 'react-redux';
import _ from 'lodash';

import listingMock from 'autoru-frontend/mockData/state/listing';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import breadcrumbsMock from 'auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock';

import type { TBreadcrumbsMarkLevel } from 'auto-core/types/TBreadcrumbs';
import { TBreadcrumbsLevelId } from 'auto-core/types/TBreadcrumbs';

import MarkCrosslinksDesktop from './MarkCrosslinksDesktop';

const defaultState = {
    listing: listingMock,
    breadcrumbsPublicApi: breadcrumbsMock,
};

it('должен показать 11 ссылок', () => {
    const tree = shallowComponent();
    expect(tree.find('.MarkCrosslinksDesktop__link')).toHaveLength(11);
    expect(tree.find('.MarkCrosslinksDesktop__link_hidden')).toHaveLength(75);
});

it('если нет тачек в продаже не рендерим провязку', () => {
    const newState = _.cloneDeep(defaultState);

    newState.breadcrumbsPublicApi.data = [ {
        entities: [ {
            id: 'AUDI',
            count: 0,
            reviews_count: 2250,
            cyrillic_name: 'Ауди',
            popular: true,
            name: 'Audi',
        } ] as unknown as TBreadcrumbsMarkLevel['entities'],
        levelFilterParams: {},
        level: TBreadcrumbsLevelId.MARK_LEVEL,
        meta_level: TBreadcrumbsLevelId.MARK_LEVEL,
        offers_count: 0,
    } ];

    const tree = shallowComponent(newState);
    expect(tree).toEqual({});
});

it('если тачек мало не рендерим кнопку показать все', () => {
    const newState = _.cloneDeep(defaultState);

    newState.breadcrumbsPublicApi.data = [ {
        entities: [ {
            id: 'AUDI',
            count: 10,
            reviews_count: 2250,
            cyrillic_name: 'Ауди',
            popular: true,
            name: 'Audi',
        } ] as unknown as TBreadcrumbsMarkLevel['entities'],
        levelFilterParams: {},
        level: TBreadcrumbsLevelId.MARK_LEVEL,
        meta_level: TBreadcrumbsLevelId.MARK_LEVEL,
        offers_count: 0,
    } ];

    const tree = shallowComponent(newState);
    expect(tree.find('Link').last().children().text().split(' ')[0]).not.toEqual('Показать');
});

it('должен правильно отработать клик по показать все и скрыть', () => {
    const tree = shallowComponent();
    tree.find('Link').last().simulate('click');
    expect(tree.find('.MarkCrosslinksDesktop__link')).toHaveLength(86);
    expect(tree.find('.MarkCrosslinksDesktop__link_hidden')).toHaveLength(0);
    tree.find('Link').last().simulate('click');
    expect(tree.find('.MarkCrosslinksDesktop__link')).toHaveLength(11);
    expect(tree.find('.MarkCrosslinksDesktop__link_hidden')).toHaveLength(75);
});

it('должен правильно формировать ссылки', () => {
    const tree = shallowComponent();
    expect(tree.find('Link').first().prop('url'))
        .toEqual('link/listing/?price_to=50000&catalog_filter=mark%3DVAZ%2Cmodel%3DKALINA&section=all&category=cars&sort=fresh_relevance_1-desc&mark=MERCEDES');
});

function shallowComponent(state = defaultState) {
    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation((selector) => selector(state));
    return shallow(
        <Provider store={ mockStore(state) }>
            <MarkCrosslinksDesktop/>
        </Provider>,
        { context: { ...contextMock } },
    ).dive();
}
