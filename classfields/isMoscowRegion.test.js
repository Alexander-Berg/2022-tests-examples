const isMoscowRegion = require('./isMoscowRegion');

const TEST_CASES = [
    {
        description: 'выбрана Московская область',
        stateMock: {
            geo: {
                gids: [ 1 ],
                geoParents: [ { id: 225 } ],
            },
        },
        result: true,
    },
    {
        description: 'выбрана Москва',
        stateMock: {
            geo: {
                gids: [ 213 ],
                geoParents: [ { id: 1 }, { id: 225 } ],
            },
        },
        result: true,
    },
    {
        description: 'выбрано Одинцово',
        stateMock: {
            geo: {
                gids: [ 10743 ],
                geoParents: [ { id: 1 }, { id: 225 } ],
            },
        },
        result: true,
    },
    {
        description: 'выбрано несколько регионов',
        stateMock: {
            geo: {
                gids: [ 213, 2 ],
            },
        },
        result: false,
    },
    {
        description: 'выбран Санкт-Петербург',
        stateMock: {
            geo: {
                gids: [ 2 ],
                geoParents: [ { id: 10174 }, { id: 225 } ],
            },
        },
        result: false,
    },
];

TEST_CASES.forEach((testCase) => {
    const { description, result, stateMock } = testCase;

    it(`Если ${ description } должен вернуть ${ result }`, () => {
        expect(isMoscowRegion(stateMock)).toEqual(result);
    });
});
