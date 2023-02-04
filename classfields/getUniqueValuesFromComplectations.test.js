const getUniqueValuesFromComplectations = require('./getUniqueValuesFromComplectations');

const COMPLECTATIONS = [
    {
        tech_info: {
            tech_param: {
                engine_type: 'GASOLINE',
            },
        },
    },
    {
        tech_info: {
            tech_param: {
                engine_type: 'GASOLINE',
            },
        },
    },
    {
        tech_info: {
            tech_param: {
                engine_type: 'DIESEL',
            },
        },
    },
    {
        tech_info: {
            tech_param: {
                engine_type: 'DIESEL',
            },
        },
    },
    {
        tech_info: {
            tech_param: {
                engine_type: 'WHATEVER',
            },
        },
    },
];

const getEngineType = require('auto-core/react/lib/complectation/getHumanEngineType');

it('должен вернуть корректный список уникальных значений двигателей из списка комплектаций', () => {
    const getEngineTypes = getUniqueValuesFromComplectations(getEngineType);
    const enginesList = getEngineTypes(COMPLECTATIONS);
    const expectedResult = [ 'Бензин', 'Дизель' ];
    expect(enginesList).toEqual(expect.arrayContaining(expectedResult));
    expect(expectedResult).toEqual(expect.arrayContaining(enginesList));
});
