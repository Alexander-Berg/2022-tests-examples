import {landingWithTlds} from 'server/seourl/tests/test-utils';

describe('Stops landing', () => {
    describe('should return stop landing by common layer name', () => {
        landingWithTlds(
            () => '/213/moscow/stops/stop_name/',
            () => ({
                id: 'stop',
                stopId: 'stop_name',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                }
            })
        );
    });

    describe('should return undefined for unknown region', () => {
        landingWithTlds(
            () => '/stops/stop_name/',
            () => undefined
        );
    });

    describe('should return undefined for stop without value', () => {
        landingWithTlds(
            () => '/213/moscow/stops/',
            () => undefined
        );
    });

    describe('should return undefined for stop with extra sublanding', () => {
        landingWithTlds(
            () => '/213/moscow/stops/stop_name/unknown',
            () => undefined
        );
    });
});
