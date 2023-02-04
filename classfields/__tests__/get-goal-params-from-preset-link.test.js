import getParams from '../';

describe('get preset goal params', () => {
    it('processes link properly', () => {
        const link = '/moskva/kupit/kvartira/';

        expect(getParams(link)).toEqual({
            preset: {
                offer_type: 'sell',
                offer_category: 'apartment',
                offer_subcategory: undefined
            }
        });
    });

    it('processes link with only location path properly', () => {
        const link = '/moskva';

        expect(getParams(link)).toEqual({
            preset: {
                offer_type: 'sell',
                offer_category: 'apartment',
                offer_subcategory: undefined
            }
        });
    });

    it('processes link with location and type properly', () => {
        const link = '/moskva/kupit';

        expect(getParams(link)).toEqual({
            preset: {
                offer_type: 'sell',
                offer_category: 'apartment',
                offer_subcategory: undefined
            }
        });
    });

    it('processes link with subcategory properly', () => {
        const link = '/moskva/snyat/kvartira/studiya';

        expect(getParams(link)).toEqual({
            preset: {
                offer_type: 'rent',
                offer_category: 'apartment',
                offer_subcategory: 'studiya'
            }
        });
    });

    it('processes link with filter properly', () => {
        const link = '/moskva/kupit/kvartira/?somefilter1=somefiltervalue&somefilter2=somefiltervalue2';

        expect(getParams(link)).toEqual({
            preset: {
                offer_type: 'sell',
                offer_category: 'apartment',
                offer_subcategory: undefined,
                filter: [
                    'somefilter1_somefiltervalue',
                    'somefilter2_somefiltervalue2'
                ]
            }
        });
    });

    it('processes link with only one filter properly', () => {
        const link = '/moskva/snyat/komnata/?somefilter1=somefiltervalue';

        expect(getParams(link)).toEqual({
            preset: {
                offer_type: 'rent',
                offer_category: 'rooms',
                offer_subcategory: undefined,
                filter: [
                    'somefilter1_somefiltervalue'
                ]
            }
        });
    });

    it('processes link ending with "?" properly', () => {
        const link = '/moskva/snyat/komnata/?';

        expect(getParams(link)).toEqual({
            preset: {
                offer_type: 'rent',
                offer_category: 'rooms',
                offer_subcategory: undefined
            }
        });
    });

    it('processes link ending with "&" properly', () => {
        const link = '/moskva/snyat/komnata/?somefilter1=somefiltervalue&';

        expect(getParams(link)).toEqual({
            preset: {
                offer_type: 'rent',
                offer_category: 'rooms',
                offer_subcategory: undefined,
                filter: [
                    'somefilter1_somefiltervalue'
                ]
            }
        });
    });

    it('processes link with filter and subcategory properly', () => {
        const link = '/moskva/kupit/kvartira/studiya/?somefilter1=somefiltervalue&somefilter2=somefiltervalue2';

        expect(getParams(link)).toEqual({
            preset: {
                offer_type: 'sell',
                offer_category: 'apartment',
                offer_subcategory: 'studiya',
                filter: [
                    'somefilter1_somefiltervalue',
                    'somefilter2_somefiltervalue2'
                ]
            }
        });
    });

    it('processes link with filter that comes with value but have no value properly', () => {
        const link = '/moskva/kupit/kvartira/studiya/?timeToMetro=&withPets=YES';

        expect(getParams(link)).toEqual({
            preset: {
                offer_type: 'sell',
                offer_category: 'apartment',
                offer_subcategory: 'studiya',
                filter: [
                    'timeToMetro_',
                    'withPets_YES'
                ]
            }
        });
    });

    it('processes link with filter that is subcategory correctly', () => {
        const link = '/moskva/kupit/dom/?houseType=HOUSE';

        expect(getParams(link)).toEqual({
            preset: {
                offer_type: 'sell',
                offer_category: 'house',
                offer_subcategory: 'house'
            }
        });
    });

    it('processes link with metro as path correctly', () => {
        const link = '/moskva/kupit/dom/metro-dostoyevskaya';

        expect(getParams(link)).toEqual({
            preset: {
                offer_type: 'sell',
                offer_category: 'house',
                filter: [ 'metro' ]
            }
        });
    });

    it('processes link with metro as query correctly', () => {
        const link = '/moskva/kupit/dom/?metroGeoId=12345';

        expect(getParams(link)).toEqual({
            preset: {
                offer_type: 'sell',
                offer_category: 'house',
                filter: [ 'metro' ]
            }
        });
    });

    it('processes link with filter that is subcategory and needs to be transformed correctly', () => {
        const link = '/moskva/kupit/garazh/?garageType=PARKING_PLACE';

        expect(getParams(link)).toEqual({
            preset: {
                offer_type: 'sell',
                offer_category: 'garage',
                offer_subcategory: 'parkingplace'
            }
        });
    });

    it('processes link with array query params correctly', () => {
        const link = '/moskva/kupit/garazh/?foo=bar&foo=baz';

        expect(getParams(link)).toEqual({
            preset: {
                offer_type: 'sell',
                offer_category: 'garage',
                filter: [ 'foo_bar', 'foo_baz' ]
            }
        });
    });
});
