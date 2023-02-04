import {expect} from 'chai';
import {yandex} from '@yandex-int/maps-proto-schemas/types';
import {getRouteLegs} from 'app/lib/response-parsers/proto/route';

describe('getRouteLegs()', () => {
    it('should return one leg and its sections', () => {
        const route = {
            geoObject: [
                {
                    metadata: [
                        {
                            '.yandex.maps.proto.driving.section.SECTION_METADATA': {
                                legIndex: 0
                            }
                        }
                    ]
                },
                {
                    metadata: [
                        {
                            '.yandex.maps.proto.driving.section.SECTION_METADATA': {
                                legIndex: 0
                            }
                        }
                    ]
                }
            ]
        } as yandex.maps.proto.common2.geo_object.IGeoObject;

        const legs = Array.from(getRouteLegs(route, '.yandex.maps.proto.driving.section.SECTION_METADATA'));
        expect(legs).to.have.length(1, 'Invalid number of route legs');
        expect(legs[0]).to.have.length(2, 'Invalid number of section in first leg');
    });

    it('should return correct number of route legs and sections ignoring waypoints', () => {
        const route = {
            geoObject: [
                {
                    geometry: [
                        {
                            point: {
                                lon: 37.565587,
                                lat: 55.743588
                            }
                        }
                    ]
                },
                {
                    metadata: [
                        {
                            '.yandex.maps.proto.masstransit.section.SECTION_METADATA': {
                                legIndex: 0
                            }
                        }
                    ]
                },
                {
                    metadata: [
                        {
                            '.yandex.maps.proto.masstransit.section.SECTION_METADATA': {
                                legIndex: 0
                            }
                        }
                    ]
                },
                {
                    geometry: [
                        {
                            point: {
                                lon: 37.566814524,
                                lat: 55.744701072
                            }
                        }
                    ]
                },
                {
                    metadata: [
                        {
                            '.yandex.maps.proto.masstransit.section.SECTION_METADATA': {
                                legIndex: 1
                            }
                        }
                    ]
                }
            ]
        } as yandex.maps.proto.common2.geo_object.IGeoObject;

        const legs = Array.from(getRouteLegs(route, '.yandex.maps.proto.masstransit.section.SECTION_METADATA'));
        expect(legs).to.have.length(2, 'Invalid number of route legs');
        expect(legs[0]).to.have.length(2, 'Invalid number of section in first leg');
        expect(legs[1]).to.have.length(1, 'Invalid number of section in second leg');
    });
});
