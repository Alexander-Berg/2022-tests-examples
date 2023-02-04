const baseInitialState = {
    geo: {
        rgid: 741965,
        locative: 'в Санкт-Петербурге',
    },
};

const stationsFirstBlock = [
    { id: 20349, name: 'Маяковская' },
    { id: 20307, name: 'Московская' },
    { id: 20310, name: 'Московские Ворота' },
];

const stationsWithDifferentCasesStationsBlock = [
    {
        id: 218430,
        name: 'ул. Дмитриевского',
    },
    {
        id: 20499,
        name: 'Улица 1905 Года',
    },
];

const spbMetroStations = [
    {
        id: 20302,
        name: 'Автово',
    },
    {
        id: 114766,
        name: 'Адмиралтейская',
    },
    {
        id: 20327,
        name: 'Академическая',
    },
    {
        id: 20344,
        name: 'Балтийская',
    },
    {
        id: 189457,
        name: 'Беговая',
    },
    {
        id: 114839,
        name: 'Бухарестская',
    },
    {
        id: 20338,
        name: 'Василеостровская',
    },
    {
        id: 20353,
        name: 'Владимирская',
    },
    {
        id: 100652,
        name: 'Волковская',
    },
    {
        id: 20330,
        name: 'Выборгская',
    },
    {
        id: 20335,
        name: 'Горьковская',
    },
    {
        id: 20348,
        name: 'Гостиный Двор',
    },
    {
        id: 20326,
        name: 'Гражданский проспект',
    },
    {
        id: 20325,
        name: 'Девяткино',
    },
    {
        id: 20352,
        name: 'Достоевская',
    },
    {
        id: 218470,
        name: 'Дунайская',
    },
    {
        id: 20315,
        name: 'Елизаровская',
    },
    {
        id: 100651,
        name: 'Звенигородская',
    },
    {
        id: 20306,
        name: 'Звёздная',
    },
    {
        id: 20303,
        name: 'Кировский Завод',
    },
    {
        id: 21743,
        name: 'Комендантский проспект',
    },
    {
        id: 20334,
        name: 'Крестовский остров',
    },
    {
        id: 20305,
        name: 'Купчино',
    },
    {
        id: 20345,
        name: 'Ладожская',
    },
    {
        id: 20301,
        name: 'Ленинский проспект',
    },
    {
        id: 20318,
        name: 'Лесная',
    },
    {
        id: 20351,
        name: 'Лиговский проспект',
    },
    {
        id: 20314,
        name: 'Ломоносовская',
    },
    {
        id: 20349,
        name: 'Маяковская',
    },
    {
        id: 114838,
        name: 'Международная',
    },
    {
        id: 20307,
        name: 'Московская',
    },
    {
        id: 20310,
        name: 'Московские Ворота',
    },
    {
        id: 20304,
        name: 'Нарвская',
    },
    {
        id: 20347,
        name: 'Невский проспект',
    },
    {
        id: 20346,
        name: 'Новочеркасская',
    },
    {
        id: 110348,
        name: 'Обводный Канал',
    },
    {
        id: 20312,
        name: 'Обухово',
    },
    {
        id: 20320,
        name: 'Озерки',
    },
    {
        id: 20308,
        name: 'Парк Победы',
    },
    {
        id: 102531,
        name: 'Парнас',
    },
    {
        id: 20336,
        name: 'Петроградская',
    },
    {
        id: 20322,
        name: 'Пионерская',
    },
    {
        id: 20350,
        name: 'Площадь Александра Невского',
    },
    {
        id: 20354,
        name: 'Площадь Восстания',
    },
    {
        id: 20331,
        name: 'Площадь Ленина',
    },
    {
        id: 20329,
        name: 'Площадь Мужества',
    },
    {
        id: 20328,
        name: 'Политехническая',
    },
    {
        id: 20337,
        name: 'Приморская',
    },
    {
        id: 20313,
        name: 'Пролетарская',
    },
    {
        id: 20317,
        name: 'Проспект Большевиков',
    },
    {
        id: 20300,
        name: 'Проспект Ветеранов',
    },
    {
        id: 20319,
        name: 'Проспект Просвещения',
    },
    {
        id: 218469,
        name: 'Проспект Славы',
    },
    {
        id: 20343,
        name: 'Пушкинская',
    },
    {
        id: 20311,
        name: 'Рыбацкое',
    },
    {
        id: 20339,
        name: 'Садовая',
    },
    {
        id: 20340,
        name: 'Сенная площадь',
    },
    {
        id: 101378,
        name: 'Спасская',
    },
    {
        id: 20332,
        name: 'Спортивная',
    },
    {
        id: 20324,
        name: 'Старая Деревня',
    },
    {
        id: 20341,
        name: 'Технологический Институт',
    },
    {
        id: 20321,
        name: 'Удельная',
    },
    {
        id: 20316,
        name: 'Улица Дыбенко',
    },
    {
        id: 20342,
        name: 'Фрунзенская',
    },
    {
        id: 20355,
        name: 'Чернышевская',
    },
    {
        id: 20333,
        name: 'Чкаловская',
    },
    {
        id: 20323,
        name: 'Чёрная Речка',
    },
    {
        id: 20309,
        name: 'Электросила',
    },
];

export const getBaseInitialStateWithOneBlock = () => ({
    ...baseInitialState,
    metroStations: {
        stations: stationsFirstBlock,
    },
});

export const getBaseInitialStateWithDifferentStationsBlock = () => ({
    ...baseInitialState,
    metroStations: {
        stations: stationsWithDifferentCasesStationsBlock,
    },
});

export const getBaseInitialStateWithoutBlocks = () => ({
    ...baseInitialState,
    metroStations: {
        stations: [],
    },
});

export const getStateWithManyStations = () => ({
    ...baseInitialState,
    metroStations: {
        stations: spbMetroStations,
    },
});
