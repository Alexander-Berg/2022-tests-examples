import {expect} from 'chai';
import {yandex} from '@yandex-int/maps-proto-schemas/types';
import proto = yandex.maps.proto;
import {formatPrecision, formatKind} from 'app/lib/geocoder/formatter';

function constructGeocoderMetaData(
    precision: proto.search.precision.Precision | null = proto.search.precision.Precision.EXACT,
    address: proto.search.address.IComponent[] = []
): proto.search.geocoder.IGeoObjectMetadata {
    return {
        housePrecision: precision,
        address: {
            formattedAddress: '',
            component: address
        }
    };
}

describe('lib/geocoder/formatter', () => {
    describe('formatPrecision', () => {
        it('should handle precision absence', () => {
            const geocoder = constructGeocoderMetaData(null);
            expect(formatKind(geocoder)).to.equal('other');
        });

        it('should handle street case correctly', () => {
            const geocoder = constructGeocoderMetaData(null, [
                {name: '', kind: [proto.search.kind.Kind.LOCALITY]},
                {name: '', kind: [proto.search.kind.Kind.STREET]}
            ]);
            expect(formatPrecision(geocoder)).to.equal('street');
        });

        it('should handle NEARBY correctly', () => {
            const geocoder = constructGeocoderMetaData(proto.search.precision.Precision.NEARBY);
            expect(formatPrecision(geocoder)).to.equal('near');
        });
    });

    describe('formatKind', () => {
        it('should handle kind absence', () => {
            const geocoder = constructGeocoderMetaData();
            expect(formatKind(geocoder)).to.equal('other');
        });
    });
});
