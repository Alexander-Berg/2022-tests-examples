import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import type { SummaryStats } from '@vertis/schema-registry/ts-types-snake/auto/api/compare_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import comparableOffersMock from 'autoru-frontend/mockData/compare/offersBase';

import compareModelMock from 'auto-core/react/dataDomain/comparableModels/mock';

import ComparisonStatsCell from './ComparisonStatsCell';

const Context = createContextProvider(contextMock);

describe('офферы', () => {
    const urlToListing = 'link/listing/?category=cars&mark=FORD&model=ECOSPORT' +
        '&super_gen=20104320&has_image=true&section=used';
    const listingParams = {
        category: 'cars',
        mark: 'FORD',
        model: 'ECOSPORT',
        super_gen: 20104320,
        has_image: true,
        section: 'used',
    };

    it('рендерит данные статистики, если они есть, и корректную ссылку', () => {
        const wrapper = renderComponent(comparableOffersMock[0].summary_stats as SummaryStats, listingParams, true);

        expect(wrapper.find('Link').prop('url')).toEqual(urlToListing);
        expect(wrapper.find('.ComparisonStatsCell__price').text()).toEqual('726 985 ₽');
        expect(wrapper.find('Link').childAt(0).text()).toEqual('70 в продаже');
    });

    it('рендерит только сслыку, если нет данных', () => {
        const wrapper = renderComponent({} as SummaryStats, listingParams, true);

        expect(wrapper.find('Link').prop('url')).toEqual(urlToListing);
        expect(wrapper.find('.ComparisonStatsCell__price').text()).toEqual('');
        expect(wrapper.find('Link').childAt(0).text()).toEqual('Объявления');
    });
});

describe('модели', () => {
    const urlToListing = 'link/listing/?category=cars&section=all&mark=SKODA&model=RAPID&super_gen=21738448' +
        '&configuration_id=21738487&tech_param_id=21738490';
    const listingParams = {
        category: 'cars',
        section: 'all',
        mark: 'SKODA',
        model: 'RAPID',
        super_gen: 21738448,
        configuration_id: 21738487,
        tech_param_id: 21738490,
    };

    it('рендерит данные статистики, если они есть, и сслыку на листинг', () => {
        const wrapper = renderComponent(compareModelMock.value().summary?.stats, listingParams, true);

        expect(wrapper.find('Link').prop('url')).toEqual(urlToListing);
        expect(wrapper.find('.ComparisonStatsCell__price').text()).toEqual('222 000 ₽');
        expect(wrapper.find('Link').childAt(0).text()).toEqual('222 в продаже');
    });

    it('рендерит только сслыку, если нет данных', () => {
        const wrapper = renderComponent(compareModelMock.withStats().value().summary?.stats, listingParams, true);

        expect(wrapper.find('Link').prop('url')).toEqual(urlToListing);
        expect(wrapper.find('.ComparisonStatsCell__price').text()).toEqual('');
        expect(wrapper.find('Link').childAt(0).text()).toEqual('Объявления');
    });

    it('если нет данных о статистике во всей строке, не рендерит часть строки с ценой', () => {
        const wrapper = renderComponent(compareModelMock.withStats().value().summary?.stats, listingParams, false);

        expect(wrapper.find('Link').prop('url')).toEqual(urlToListing);
        expect(wrapper.find('.ComparisonStatsCell__price')).not.toExist();
        expect(wrapper.find('Link').childAt(0).text()).toEqual('Объявления');
    });
});

function renderComponent(statsMock: SummaryStats | undefined, listingParams: Record<string, any>, hasStatsData: boolean) {
    return shallow(
        <Context>
            <ComparisonStatsCell
                stats={ statsMock }
                listingParams={ listingParams }
                hasStatsData={ hasStatsData }
            />
        </Context>,
    ).dive();
}
