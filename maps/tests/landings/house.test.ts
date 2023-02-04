import {encrypt, decrypt} from 'server/seourl/encryption';
import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('House landing', () => {
    describe('should return house landing', () => {
        landingWithTlds(
            () => [
                '/213/moscow/house/house_seoname/Z00YcgVjS0YbWFt0YX9zcng=/',
                '/213/moscow/house/house_seoname/34.42323,45.23234/'
            ],
            () => ({
                id: 'house',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                seoname: 'house_seoname',
                coordinates: [34.42323, 45.23234]
            })
        );
        landingWithTlds(
            () => ['/213/moscow/house/Z00YcgVjS0YbWFt0YX9zcng=/', '/213/moscow/house/34.42323,45.23234/'],
            () => ({
                id: 'house',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                },
                seoname: undefined,
                coordinates: [34.42323, 45.23234]
            })
        );
    });

    const AVAILABLE_SUBLANDINGS = ['inside', 'panorama', 'gallery'] as const;
    AVAILABLE_SUBLANDINGS.forEach((sublanding) => {
        describe(`should return house landing with ${sublanding} sublanding`, () => {
            landingWithTlds(
                () => [
                    `/213/moscow/house/house_seoname/Z00YcgVjS0YbWFt0YX9zcng=/${sublanding}/`,
                    `/213/moscow/house/house_seoname/34.42323,45.23234/${sublanding}/`
                ],
                () => ({
                    id: 'house',
                    region: {
                        regionId: 213,
                        seoname: 'moscow'
                    },
                    seoname: 'house_seoname',
                    coordinates: [34.42323, 45.23234],
                    sublanding
                })
            );
            landingWithTlds(
                () => [
                    `/213/moscow/house/Z00YcgVjS0YbWFt0YX9zcng=/${sublanding}/`,
                    `/213/moscow/house/34.42323,45.23234/${sublanding}/`
                ],
                () => ({
                    id: 'house',
                    region: {
                        regionId: 213,
                        seoname: 'moscow'
                    },
                    seoname: undefined,
                    coordinates: [34.42323, 45.23234],
                    sublanding
                })
            );
        });
    });

    describe('should return undefined for malformed coordinates', () => {
        landingWithTlds(
            () => [
                '/213/moscow/house/house_seoname/ZeeecgVjS0YbWFt0YX9zcng=/',
                '/213/moscow/house/house_seoname/34.42323%245.23234/'
            ],
            () => undefined
        );
    });

    describe('should return undefined for one coordinate', () => {
        landingWithTlds(
            () => ['/213/moscow/house/house_seoname/Z00YcgVjS0Y=/', '/213/moscow/house/house_seoname/34.42323/'],
            () => undefined
        );
    });

    describe('should return undefined for unknown sublanding', () => {
        landingWithTlds(
            () => [
                '/213/moscow/house/house_seoname/Z00YcgVjS0YbWFt0YX9zcng=/unknown',
                '/213/moscow/house/house_seoname/34.42323,45.23234/unknown'
            ],
            () => undefined
        );
    });
});

it('House coordinates should be stable encrypted', () => {
    expect(encrypt([34.42323, 45.23234])).toEqual('Z00YcgVjS0YHQFpvfX5yc39nZA==');
    expect(decrypt('Z00YcgVjS0YHQFpvfX5yc39nZA==')).toEqual([34.42323, 45.23234]);
});
