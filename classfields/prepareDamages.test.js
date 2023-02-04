const prepareDamages = require('./prepareDamages');

const TEST_CASES = [
    {
        description: 'переданы повреждения, указанные продавцом',
        parameters: {
            damages: [
                { car_part: 'REAR_BUMPER', type: [ 'DYED' ] },
                { car_part: 'FRONT_BUMPER', type: [ 'DYED' ] },
            ],
        },
        result: [
            {
                car_part: 'REAR_BUMPER',
                message: 'Окрашено',
                title: 'Задний бампер',
            },
            {
                car_part: 'FRONT_BUMPER',
                message: 'Окрашено',
                title: 'Передний бампер',
            },
        ],
    },
    {
        description: 'переданы некорректные повреждения, указанные продавцом',
        parameters: {
            damages: [
                { car_part: 'WHAT_?', type: [ 'DYED' ] },
                { car_part: 'FRONT_BUMPER', type: [ 'DYED' ] },
            ],
        },
        result: [
            {
                car_part: 'WHAT_?',
                description: '',
                message: 'Окрашено',
            },
            {
                car_part: 'FRONT_BUMPER',
                message: 'Окрашено',
                title: 'Передний бампер',
            },
        ],
    },
    {
        description: 'не переданы повреждения',
        parameters: {},
        result: [],
    },
];

TEST_CASES.forEach((test) => {
    it(`Должен вернуть правильный результат, если ${ test.description }`, () => {
        expect(prepareDamages(test.parameters.damages)).toEqual(test.result);
    });
});
