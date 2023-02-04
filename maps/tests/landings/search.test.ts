import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Search landing', () => {
    describe('should return search landing for link', () => {
        landingWithTlds(
            () => '/213/moscow/search/Дон массажио',
            () => ({
                id: 'search',
                value: 'Дон массажио',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                }
            })
        );
    });

    describe('should return search landing for link with region and boolean filter', () => {
        landingWithTlds(
            () => '/213/moscow/search/кафе/filter/open_now',
            () => ({
                id: 'search',
                value: 'кафе',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                filter: {
                    name: 'open_now'
                }
            })
        );
    });

    describe('should return search landing for link with region and enum filter', () => {
        landingWithTlds(
            () => '/213/moscow/search/кафе/filter/category_id/184108079',
            () => ({
                id: 'search',
                value: 'кафе',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                filter: {
                    name: 'category_id',
                    value: '184108079'
                }
            })
        );
    });

    describe('should return undefined for link with no search text', () => {
        landingWithTlds(
            () => '213/moscow/search',
            () => undefined
        );
    });

    describe('should return undefined for link with no region', () => {
        landingWithTlds(
            () => 'search/Дон массажио',
            () => undefined
        );
    });

    describe('should return undefined for missed filter name', () => {
        landingWithTlds(
            () => '/213/moscow/search/кафе/filter',
            () => undefined
        );
    });
});
