const flattenFormFieldsData = require('./flattenFormFieldsData');

it('should flatten formFields.data', () => {
    expect(flattenFormFieldsData({
        mark: { value: 'AUDI' },
        model: { value: 'A4' },
    })).toEqual({ mark: 'AUDI', model: 'A4' });
});

it('should flatten formFields.data (complex test)', () => {
    expect(flattenFormFieldsData(
        /* eslint-disable */
        JSON.parse('{"category":{},"section":{"value":"used"},"mark":{"value":"BMW"},"model":{"value":"3ER"},"currency":{"value":"RUR"},"price":{},"color":{"value":"040001"},"address":{"value":{"geo_id":213,"cityName":"Москва","address":null,"coordinates":[55.75396,37.620393]}},"phones":{"value":[]},"pts":{"value":"ORIGINAL"},"description":{},"fingerprint":{},"run":{"value":120000},"year":{"value":"2012"},"body_type":{"value":"SEDAN"},"super_gen":{"value":"7744658"},"engine_type":{"value":"DIESEL"},"gear_type":{"value":"REAR_DRIVE"},"transmission_full":{"value":"AUTOMATIC"},"tech_param":{"value":"9289777"},"aux":{"value":true},"audiosystem":{"value":"audiosystem-cd"},"ashtray-and-cigarette-lighter":{"value":true},"computer":{"value":true},"cooling-box":{"value":true},"driver-seat-updown":{"value":true},"electro-window-all":{"value":true},"park-assist-f":{"value":false},"buy_year_evaluation":{"value":2016,"touched":true},"owners_count":{"value":1,"touched":true}}')
        /* eslint-enable */
    )).toEqual({
        category: undefined,
        section: 'used',
        mark: 'BMW',
        model: '3ER',
        currency: 'RUR',
        price: undefined,
        color: '040001',
        address:
            { geo_id: 213,
                cityName: 'Москва',
                address: null,
                coordinates: [ 55.75396, 37.620393 ] },
        phones: [],
        pts: 'ORIGINAL',
        description: undefined,
        fingerprint: undefined,
        run: 120000,
        year: '2012',
        body_type: 'SEDAN',
        super_gen: '7744658',
        engine_type: 'DIESEL',
        gear_type: 'REAR_DRIVE',
        transmission_full: 'AUTOMATIC',
        tech_param: '9289777',
        aux: true,
        audiosystem: 'audiosystem-cd',
        'ashtray-and-cigarette-lighter': true,
        computer: true,
        'cooling-box': true,
        'driver-seat-updown': true,
        'electro-window-all': true,
        'park-assist-f': false,
        owners_count: 1,
        buy_year_evaluation: 2016,
    });
});
