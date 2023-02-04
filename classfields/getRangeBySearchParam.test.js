jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const getRangeBySearchParam = require('./getRangeBySearchParam');

const getResource = require('auto-core/react/lib/gateApi').getResource;

const gateApiMock = (resource, params) => {
    if (params.sort.endsWith('-ASC')) {
        return Promise.resolve({
            offers: [
                {
                    documents: {
                        year: 2000,
                    },
                },
            ],
        });
    }
    return Promise.resolve({
        offers: [
            {
                documents: {
                    year: 2016,
                },
            },
        ],
    });

};
getResource.mockImplementation(gateApiMock);

it('должен вернуть границы диапазона для года выпуска', () => {
    const searchParams = {};
    const paramName = 'year';
    return getRangeBySearchParam(searchParams, paramName).then(
        result => {
            expect(result).toStrictEqual([ 2000, 2016 ]);
        },
    );
});
