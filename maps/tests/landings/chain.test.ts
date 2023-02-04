import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Chain landing', () => {
    describe('should return chain landing for link with region', () => {
        landingWithTlds(
            () => '/213/moscow/chain/seoname/1235670',
            () => ({
                id: 'chain',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                seoname: 'seoname',
                chainId: 1235670
            })
        );
    });

    describe('should return chain landing for link with region and boolean filter', () => {
        landingWithTlds(
            () => '/213/moscow/chain/seoname/1235670/filter/open_now',
            () => ({
                id: 'chain',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                seoname: 'seoname',
                chainId: 1235670,
                filter: {
                    name: 'open_now'
                }
            })
        );
    });

    describe('should return chain landing for link with region and enum filter', () => {
        landingWithTlds(
            () => '/213/moscow/chain/seoname/1235670/filter/category_id/184108079',
            () => ({
                id: 'chain',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                seoname: 'seoname',
                chainId: 1235670,
                filter: {
                    name: 'category_id',
                    value: '184108079'
                }
            })
        );
    });

    describe('should return undefined for link without region', () => {
        landingWithTlds(
            () => '/chain/seoname/1235670',
            () => undefined
        );
    });

    describe('should return undefined for link without seoname and chain id', () => {
        landingWithTlds(
            () => '/213/moscow/chain',
            () => undefined
        );
    });

    describe('should return undefined for link without chain id', () => {
        landingWithTlds(
            () => '/213/moscow/chain/seoname',
            () => undefined
        );
    });

    describe('should return undefined for link without seoname', () => {
        landingWithTlds(
            () => '/213/moscow/chain/12345670',
            () => undefined
        );
    });

    describe('should return undefined for link with extra sublanding', () => {
        landingWithTlds(
            () => '/213/moscow/chain/seoname/12345670/unknown',
            () => undefined
        );
    });

    describe('should return undefined for missed filter name', () => {
        landingWithTlds(
            () => '/213/moscow/chain/seoname/12345670/filter',
            () => undefined
        );
    });
});
