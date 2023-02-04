import MockDate from 'mockdate';

import {
    getSearchSaleEventDefaultStructuredData,
    getSearchSaleEventSitesStructuredData,
} from '../getSearchSaleEventStructuredData';
import { getNewBuildingsStructuredData } from '../getNewBuildingsSaleEventStructuredData';

import { mockState } from './mocks/mocks.index';

describe('sale event', () => {
    beforeEach(() => {
        MockDate.set('2021-12-01T21:00');
    });

    afterEach(() => {
        MockDate.reset();
    });

    it('search default', () => {
        getSearchSaleEventDefaultStructuredData().forEach((searchSaleEventDefaultStructuredData) => {
            expect(searchSaleEventDefaultStructuredData).toMatchSnapshot();
        });
    });

    it('search sites', () => {
        getSearchSaleEventSitesStructuredData().forEach((searchSaleEventSitesStructuredData) => {
            expect(searchSaleEventSitesStructuredData).toMatchSnapshot();
        });
    });

    it('newbuilding proposals', () => {
        getNewBuildingsStructuredData(mockState)?.forEach((newBuildingsStructuredData) => {
            expect(newBuildingsStructuredData).toMatchSnapshot();
        });
    });
});
