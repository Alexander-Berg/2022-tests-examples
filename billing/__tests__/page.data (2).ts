export const requestsResponse = {
    items: [
        {
            id: 1,
            dt: '2021-12-23T00:00:00',
            person: {
                id: 123,
                name: 'Рога и копыта',
                inn: '1234567890'
            },
            firm: {
                id: 1,
                label: 'ООО ЯНДЕКС'
            },
            contract: {
                id: 123,
                externalId: '123',
                dt: '2022-01-01T00:00:00',
                finishDt: '2022-12-01T00:00:00'
            },
            dtRange: {
                dtFrom: '2020-12-23T00:00:00',
                dtTo: '2021-12-23T00:00:00'
            },
            email: 'asd@asd.asd',
            status: 'FAILED'
        },
        {
            id: 2,
            dt: '2021-12-23T00:00:00',
            person: {
                id: 345,
                name: 'Рога и копыта',
                inn: '1234567890'
            },
            firm: {
                id: 123,
                label: 'ООО ЯНДЕКС'
            },
            contract: {
                id: 234,
                externalId: '123',
                dt: '2022-01-01T00:00:00',
                finishDt: '2022-12-01T00:00:00'
            },
            dtRange: {
                dtFrom: '2020-12-23T00:00:00',
                dtTo: '2021-12-23T00:00:00'
            },
            email: 'asd@asd.asd',
            status: 'NEW'
        }
    ],
    totalCount: 2
};

export const personsFirmsResponse = {
    items: [
        {
            firm: {
                id: 123,
                label: '1я фирма плательщика 1'
            },
            contract: {
                dt: '2021-04-19T00:00:00',
                externalId: '1839660/1',
                finishDt: '2021-04-20T00:00:00',
                id: 234
            },
            person: {
                id: 345,
                name: 'плательщик 1',
                inn: '1234567890',
                email: 'asd@asd.asd'
            }
        },
        {
            firm: {
                id: 234,
                label: '2я фирма плательщика 1'
            },
            contract: null,
            person: {
                id: 345,
                name: 'плательщик 1',
                inn: '1234567890',
                email: 'asd@asd.asd'
            }
        }
    ]
};

export const lastClosedPeriodsResponse = {
    items: [
        {
            firmId: 123,
            lastClosedDt: '2022-05-31T00:00:00'
        }
    ]
};

export const requestResponse = {
    data: {
        id: 3,
        dt: '2021-12-23T00:00:00',
        person: {
            id: 345,
            name: 'Рога и копыта',
            inn: '1234567890'
        },
        firm: {
            id: 123,
            label: 'ООО ЯНДЕКС'
        },
        contract: {
            id: 234,
            externalId: '123',
            dt: '2022-01-01T00:00:00',
            finishDt: '2022-12-01T00:00:00'
        },
        dtRange: {
            dtFrom: '2020-12-23T00:00:00',
            dtTo: '2021-12-23T00:00:00'
        },
        email: 'asd@asd.asd',
        status: 'NEW'
    }
};
