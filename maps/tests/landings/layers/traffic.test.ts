import {Tld} from 'types/locale';
import {getLocalLayerName} from 'utils/urls';
import {landingWithTlds, withTlds} from 'server/seourl/tests/test-utils';

// TODO: Проверить список
const TRAFFIC_ALIASES: Record<Tld, string> = {
    ru: 'probki',
    ua: 'probki',
    ge: 'probki',
    uz: 'probki',
    by: 'probki',
    kz: 'probki',
    kg: 'probki',
    lv: 'probki',
    lt: 'probki',
    tj: 'probki',
    tm: 'probki',
    md: 'probki',
    ee: 'probki',
    com: 'traffic',
    eu: 'traffic',
    fr: 'traffic',
    tr: 'trafik',
    il: 'traffic',
    az: 'traffic',
    fi: 'traffic',
    pl: 'traffic'
};

describe('Traffic layer', () => {
    describe('should return traffic layer by common and local layer names', () => {
        landingWithTlds(
            (tld) => ['/traffic', `/${TRAFFIC_ALIASES[tld]}`],
            () => ({
                id: 'layer',
                name: 'traffic'
            })
        );
    });

    describe('should return undefined by local layer name with region (sometimes?)', () => {
        landingWithTlds(
            () => ['/moscow_traffic', '/moscow_trafik', '/moscow_probki'],
            () => ({
                id: 'layer',
                name: 'traffic',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                }
            })
        );
    });

    describe('should return traffic layer by common layer name', () => {
        landingWithTlds(
            (tld) => `/213/moscow/${TRAFFIC_ALIASES[tld]}`,
            () => ({
                id: 'layer',
                name: 'traffic',
                region: {
                    regionId: 213,
                    seoname: 'moscow'
                }
            })
        );
    });

    describe('should return correct local alias name', () => {
        withTlds(
            (tld) => [getLocalLayerName('traffic', tld)],
            (tld) => TRAFFIC_ALIASES[tld]
        );
    });

    describe('should return undefined for unknown region shortcut', () => {
        landingWithTlds(
            () => '/unknown_traffic',
            () => undefined
        );
    });

    describe('should return undefined by common layer name with sublanding', () => {
        landingWithTlds(
            (tld) => `/213/moscow/${TRAFFIC_ALIASES[tld]}/unknown`,
            () => undefined
        );
    });
});
