import {expect} from 'chai';
import {yandex} from '@yandex-int/maps-proto-schemas/types';
import proto = yandex.maps.proto;
import {formatAddressDetails} from 'app/lib/geocoder/address-details';
import * as input from '../fixtures/address-to-xal.input.json';
import * as expected from '../fixtures/address-to-xal.expected.json';

const kindToProtoKind: Record<string, number> = {
    UNKNOWN: 32,
    COUNTRY: 0,
    REGION: 1,
    PROVINCE: 2,
    AREA: 3,
    LOCALITY: 4,
    DISTRICT: 5,
    STREET: 6,
    HOUSE: 7,
    ROUTE: 8,
    STATION: 9,
    METRO_STATION: 10,
    RAILWAY_STATION: 11,
    VEGETATION: 12,
    HYDRO: 13,
    AIRPORT: 14,
    OTHER: 15,
    ENTRANCE: 16
};

describe('lib/geocoder/address-details', () => {
    input.forEach((testCase, index) => {
        it(testCase.formattedAddress, () => {
            const addressDetails = formatAddressDetails({
                formattedAddress: testCase.formattedAddress,
                countryCode: testCase.countryCode,
                component: testCase.component.map((component) => ({
                    name: component.name,
                    kind: component.kind.map((kind) => kindToProtoKind[kind] as proto.search.kind.Kind)
                }))
            });
            expect(addressDetails).to.deep.equals(expected[index]);
        });
    });
});
