export const geo = {
    rgid: 587795,
    locative: 'в Москве'
};

export const getStreets = () => ({
    letters: [
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '8',
        '9',
        'а',
        'б',
        'в',
        'г',
        'д',
        'е',
        'ж',
        'з',
        'и',
        'к',
        'л',
        'м',
        'н',
        'о',
        'п',
        'р',
        'с',
        'т',
        'у',
        'ф',
        'х',
        'ц',
        'ч',
        'ш',
        'щ',
        'э',
        'ю',
        'я'
    ],
    page: 1,
    totalPages: 2,
    items: [
        {
            streetName: '3-я Фрунзенская улица',
            streetId: [
                {
                    id: 6205,
                    streetAddress: 'Россия, Москва, 3-я Фрунзенская улица'
                }
            ]
        },
        {
            streetName: '3-я Хорошёвская улица',
            streetId: [
                {
                    id: 124609,
                    streetAddress: 'Россия, Москва, 3-я Хорошёвская улица'
                }
            ]
        },
        {
            streetName: 'Самокатная улица',
            streetId: [
                {
                    id: 112955,
                    streetAddress: 'Россия, Москва, Самокатная улица'
                }
            ]
        },
        {
            streetName: 'улица Саморы Машела',
            streetId: [
                {
                    id: 75055,
                    streetAddress: 'Россия, Москва, улица Саморы Машела'
                }
            ]
        },
        {
            streetName: 'Самотёчная улица',
            streetId: [
                {
                    id: 90222,
                    streetAddress: 'Россия, Москва, Самотёчная улица'
                }
            ]
        },
        {
            streetName: 'улица Самуила Маршака',
            streetId: [
                {
                    id: 135810,
                    streetAddress: 'Россия, Москва, поселение Внуковское, улица Самуила Маршака'
                }
            ]
        },
        {
            streetName: 'Светлый бульвар',
            streetId: [
                {
                    id: 175843,
                    streetAddress: 'Россия, Москва, поселение Филимонковское, посёлок Марьино, Светлый бульвар'
                }
            ]
        }
    ]
});

export const getFilteredStreets = () => {
    const { letters, items } = getStreets();

    return {
        letter: '3',
        letters,
        page: 1,
        totalPages: 1,
        items: items.slice(0, 2)
    };
};
