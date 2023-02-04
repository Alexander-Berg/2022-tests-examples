import {expect} from 'chai';
import {yandex} from '@yandex-int/maps-proto-schemas/types';

import {addressToXal} from '../../../app/lib/address-to-xal';

import * as inputData from '../fixtures/address-to-xal.input.json';
import * as expected from '../fixtures/address-to-xal.expected.json';

import ProtoAddress = yandex.maps.proto.search.address.IAddress;
import ProtoKind = yandex.maps.proto.search.kind.Kind;

type Kind =
    'UNKNOWN' |
    'COUNTRY' |
    'REGION' |
    'PROVINCE' |
    'AREA' |
    'LOCALITY' |
    'DISTRICT' |
    'STREET' |
    'HOUSE' |
    'ROUTE' |
    'STATION' |
    'METRO_STATION' |
    'RAILWAY_STATION' |
    'VEGETATION' |
    'HYDRO' |
    'AIRPORT' |
    'OTHER' |
    'ENTRANCE';

interface InputAddressComponent {
    name: string;
    kind: [Kind];
}

interface Address {
    formattedAddress: string;
    countryCode: string;
    component: InputAddressComponent[];
}

const transformKind = (kindData: [Kind]): [ProtoKind] => {
    const kind = kindData[0];

    switch (kind) {
        case 'METRO_STATION':
            return [ProtoKind.METRO_STATION];
        case 'COUNTRY':
            return [ProtoKind.COUNTRY];
        case 'REGION':
            return [ProtoKind.REGION];
        case 'PROVINCE':
            return [ProtoKind.PROVINCE];
        case 'AREA':
            return [ProtoKind.AREA];
        case 'LOCALITY':
            return [ProtoKind.LOCALITY];
        case 'DISTRICT':
            return [ProtoKind.DISTRICT];
        case 'STREET':
            return [ProtoKind.STREET];
        case 'HOUSE':
            return [ProtoKind.HOUSE];
        case 'ROUTE':
            return [ProtoKind.ROUTE];
        case 'STATION':
            return [ProtoKind.STATION];
        case 'RAILWAY_STATION':
            return [ProtoKind.RAILWAY_STATION];
        case 'VEGETATION':
            return [ProtoKind.VEGETATION];
        case 'HYDRO':
            return [ProtoKind.HYDRO];
        case 'AIRPORT':
            return [ProtoKind.AIRPORT];
        case 'ENTRANCE':
            return [ProtoKind.ENTRANCE];
        case 'OTHER':
        case 'UNKNOWN':
        default:
            return [ProtoKind.OTHER];
    }
};

const prepare = (address: Address[]): ProtoAddress[] => {
    return address.map(({formattedAddress, countryCode, component}): ProtoAddress => {
        const result: ProtoAddress = {formattedAddress, countryCode};

        result.component = component.map(({kind, name}) => ({
            name,
            kind: transformKind(kind)
        }));

        return result;
    });
};

const input: ProtoAddress[] = prepare(inputData as Address[]);

describe('addressToXal()', () => {
    it('should corrent transform address to xAL format', () => {
        input.forEach((inputItem, i) => {
            const address = addressToXal(inputItem);
            expect(address).to.deep.equal(expected[i]);
        });
    });
});
