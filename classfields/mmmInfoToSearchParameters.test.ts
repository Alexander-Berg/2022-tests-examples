import mmmInfoToSearchParameters from './mmmInfoToSearchParameters';

it('должен правильно перевести mmmInfo в searchParameters', () => {
    const testCase = [
        {
            exclude: false,
            mark: 'BMW',
            models: [ {
                id: 'M5',
                generations: [ '2873648273' ],
                nameplates: [ '129841724' ],
            } ],
        },
        {
            exclude: true,
            mark: 'VAZ',
            models: [ {
                id: 'XRAY',
                generations: [ '2873648273' ],
                nameplates: [ '129841724' ],
            } ],
        },
    ];

    const expectedObject = {
        catalog_filter: [
            {
                generation: '2873648273',
                mark: 'BMW',
                model: 'M5',
                nameplate_name: '129841724',
            },
        ],
        exclude_catalog_filter: [
            {
                generation: '2873648273',
                mark: 'VAZ',
                model: 'XRAY',
                nameplate_name: '129841724',
            },
        ],
    };

    expect(mmmInfoToSearchParameters(testCase)).toMatchObject(expectedObject);
});
