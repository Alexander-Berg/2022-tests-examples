import getActualEquipment from './getActualEquipment';

const state = {
    equipmentFilters: {
        data: {
            categories: [
                {
                    name: 'Обзор',
                    groups: [
                        {
                            name: 'Фары',
                            options: [
                                {
                                    name: 'Ксеноновые/Биксеноновые',
                                    code: 'xenon',
                                    offers_count: 10,
                                    full_name: 'Ксеноновые/Биксеноновые фары',
                                },
                                {
                                    name: 'Лазерные',
                                    code: 'laser-lights',
                                    offers_count: 10,
                                    full_name: 'Лазерные фары',
                                },
                                {
                                    name: 'Светодиодные',
                                    code: 'led-lights',
                                    offers_count: 0,
                                    full_name: 'Светодиодные фары',
                                },
                            ],
                            code: 'lights',
                        },
                    ],
                },
            ],
        },
    },
};

it('должен вернуть актуальный список опций', () => {
    expect(
        getActualEquipment(state),
    ).toEqual(
        [
            {
                group: 'Фары',
                code: 'xenon',
                name: 'Ксеноновые/Биксеноновые фары',
            },
            {
                group: 'Фары',
                code: 'laser-lights',
                name: 'Лазерные фары',
            },
        ],
    );
});
