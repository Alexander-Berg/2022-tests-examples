const generationGallery = require('./generationGallery');

describe('catalog-listing-specifications', () => {
    it('должен построить наименование рестайлинг', () => {
        const result = generationGallery([ {
            'autoru-body-types': [ 'CABRIO', 'COUPE' ],
            'body-types': [ 'CABRIOLET', 'COUPE' ],
            'closed-generation': false,
            'configuration-id': '21089365',
            'generation-id': '21089332',
            group: 'FAMILY',
            'has-complectations': false,
            'has-open-complectations': false,
            name: 'VI',
            preview: '//avatars.mds.yandex.net/get-verba/1030388/2a0000016091258753fd67ec4d2937f800ff/gallery',
            'preview-cattouchret': '//avatars.mds.yandex.net/get-verba/1030388/2a0000016091258753fd67ec4d2937f800ff/cattouchret',
            restyle: true,
            segment: 'MEDIUM',
            'super-generation-id': '21089327',
            'year-from': 2017,
            'year-to': 2022,
        } ],
        { mark: { id: 'FORD', name: 'Ford' }, model: { id: 'MUSTANG', name: 'Mustang' } },
        {});

        expect(result[0]).toHaveProperty('name', 'VI рестайлинг');
    });

    it('должен построить наименование без рестайлинга', () => {
        const result = generationGallery([ {
            'autoru-body-types': [ 'CABRIO', 'COUPE' ],
            'body-types': [ 'CABRIOLET', 'COUPE' ],
            'closed-generation': false,
            'configuration-id': '21089365',
            'generation-id': '21089332',
            group: 'FAMILY',
            'has-complectations': false,
            'has-open-complectations': false,
            name: 'VI',
            preview: '//avatars.mds.yandex.net/get-verba/1030388/2a0000016091258753fd67ec4d2937f800ff/gallery',
            'preview-cattouchret': '//avatars.mds.yandex.net/get-verba/1030388/2a0000016091258753fd67ec4d2937f800ff/cattouchret',
            restyle: false,
            segment: 'MEDIUM',
            'super-generation-id': '21089327',
            'year-from': 2017,
            'year-to': 2022,
        } ],
        { mark: { id: 'FORD', name: 'Ford' }, model: { id: 'MUSTANG', name: 'Mustang' } },
        {});

        expect(result[0]).toHaveProperty('name', 'VI');
    });
});
