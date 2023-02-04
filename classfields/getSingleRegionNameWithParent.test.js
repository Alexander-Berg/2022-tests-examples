const getSingleRegionNameWithParent = require('./getSingleRegionNameWithParent').default;

const TEST_CASES = [
    {
        description: 'выбрана Москва',
        stateMock: {
            geo: {
                gids: [ 213 ],
                geoParents: [ { id: 1 }, { id: 225 } ],
                gidsInfo: [
                    {
                        type: 6,
                        linguistics: {
                            ablative: '',
                            accusative: 'Москву',
                            dative: 'Москве',
                            directional: '',
                            genitive: 'Москвы',
                            instrumental: 'Москвой',
                            locative: '',
                            nominative: 'Москва',
                            preposition: 'в',
                            prepositional: 'Москве',
                        },
                    },
                ],
            },
        },
    },
    {
        description: 'выбран Кировский район',
        stateMock: {
            geo: {
                gids: [ 98822 ],
                geoParents: [
                    {
                        id: 10693,
                        linguistics: {
                            ablative: '',
                            accusative: 'Калужскую область',
                            dative: 'Калужской области',
                            directional: '',
                            genitive: 'Калужской области',
                            instrumental: 'Калужской областью',
                            locative: '',
                            nominative: 'Калужская область',
                            preposition: 'в',
                            prepositional: 'Калужской области',
                        },
                    },
                    {
                        id: 225,
                    },
                ],
                gidsInfo: [ {
                    type: 10,
                    id: 98822,
                    linguistics: {
                        ablative: '',
                        accusative: 'Кировский район',
                        dative: 'Кировскому району',
                        directional: '',
                        genitive: 'Кировского района',
                        instrumental: 'Кировским районом',
                        locative: '',
                        nominative: 'Кировский район',
                        preposition: 'в',
                        prepositional: 'Кировском районе',
                    },
                } ],
            },
        },
    },
];

TEST_CASES.forEach((testCase) => {
    const { description, stateMock } = testCase;

    it(`Правильно рисует гео с родителем только для районов - ${ description }`, () => {
        expect(getSingleRegionNameWithParent(stateMock)).toMatchSnapshot();
    });
});
