import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Category landing', () => {
    describe('should return category landing for link with region', () => {
        landingWithTlds(
            () => '/213/moscow/category/cafe',
            () => ({
                id: 'category',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                seoname: 'cafe'
            })
        );
        landingWithTlds(
            () => '/213/moscow/category/cafe/555',
            () => ({
                id: 'category',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                categoryId: 555,
                seoname: 'cafe'
            })
        );
    });

    describe('should return category landing for link with region and geo context', () => {
        landingWithTlds(
            () => '/213/moscow/geo/park_kultury/29052341/category/cafe',
            () => ({
                id: 'category',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                seoname: 'cafe',
                geo: {
                    id: 29052341,
                    seoname: 'park_kultury'
                }
            })
        );
        landingWithTlds(
            () => '/213/moscow/geo/park_kultury/29052341/category/cafe/555',
            () => ({
                id: 'category',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                categoryId: 555,
                seoname: 'cafe',
                geo: {
                    id: 29052341,
                    seoname: 'park_kultury'
                }
            })
        );
    });

    describe('should return category landing for link with region and filter', () => {
        landingWithTlds(
            () => '/213/moscow/category/cafe/filter/italiane_cuisune',
            () => ({
                id: 'category',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                seoname: 'cafe',
                filter: {
                    name: 'italiane_cuisune'
                }
            })
        );
        landingWithTlds(
            () => '/213/moscow/category/cafe/555/filter/italiane_cuisune',
            () => ({
                id: 'category',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                categoryId: 555,
                seoname: 'cafe',
                filter: {
                    name: 'italiane_cuisune'
                }
            })
        );
    });

    describe('should return category landing for link with region and enum filter', () => {
        landingWithTlds(
            () => '/213/moscow/category/cafe/filter/category_id/184108079',
            () => ({
                id: 'category',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                seoname: 'cafe',
                filter: {
                    name: 'category_id',
                    value: '184108079'
                }
            })
        );
        landingWithTlds(
            () => '/213/moscow/category/cafe/555/filter/category_id/184108079',
            () => ({
                id: 'category',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                categoryId: 555,
                seoname: 'cafe',
                filter: {
                    name: 'category_id',
                    value: '184108079'
                }
            })
        );
    });

    describe('should return category landing for link with region, geo context and filter', () => {
        landingWithTlds(
            () => '/213/moscow/geo/park_kultury/29052341/category/cafe/filter/italiane_cuisune',
            () => ({
                id: 'category',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                seoname: 'cafe',
                geo: {
                    id: 29052341,
                    seoname: 'park_kultury'
                },
                filter: {
                    name: 'italiane_cuisune'
                }
            })
        );
        landingWithTlds(
            () => '/213/moscow/geo/park_kultury/29052341/category/cafe/555/filter/italiane_cuisune',
            () => ({
                id: 'category',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                categoryId: 555,
                seoname: 'cafe',
                geo: {
                    id: 29052341,
                    seoname: 'park_kultury'
                },
                filter: {
                    name: 'italiane_cuisune'
                }
            })
        );
    });

    describe('should return category landing for link with missed geo seoname', () => {
        landingWithTlds(
            () => '/213/moscow/geo/29052341/category/cafe',
            () => ({
                id: 'category',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                seoname: 'cafe',
                geo: {
                    id: 29052341
                }
            })
        );
        landingWithTlds(
            () => '/213/moscow/geo/29052341/category/cafe/555',
            () => ({
                id: 'category',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                categoryId: 555,
                seoname: 'cafe',
                geo: {
                    id: 29052341
                }
            })
        );
    });

    describe('should return undefined for missed cafe name', () => {
        landingWithTlds(
            () => '/213/moscow/category',
            () => undefined
        );
    });

    describe('should return undefined for missed geo id', () => {
        landingWithTlds(
            () => '/213/moscow/geo/park_kultury/category/cafe',
            () => undefined
        );
        landingWithTlds(
            () => '/213/moscow/geo/park_kultury/category/cafe/555',
            () => undefined
        );
    });

    describe('should return undefined for missed geo keyword', () => {
        landingWithTlds(
            () => '/213/moscow/park_kultury/29052341/category/cafe',
            () => undefined
        );
        landingWithTlds(
            () => '/213/moscow/park_kultury/29052341/category/cafe/555',
            () => undefined
        );
    });

    describe('should return undefined for geo without category', () => {
        landingWithTlds(
            () => '/213/moscow/geo/park_kultury',
            () => undefined
        );
    });

    describe('should return undefined for missed filter name', () => {
        landingWithTlds(
            () => '/213/moscow/category/cafe/filter',
            () => undefined
        );
        landingWithTlds(
            () => '/213/moscow/category/cafe/555/filter',
            () => undefined
        );
    });
});
