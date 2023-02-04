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

const stationsSecondBlock = [
    { id: 20340, name: 'Сенная площадь' },
    { id: 20324, name: 'Старая деревня' },
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

export const getBaseInitialStateWithOneBlock = () => ({
    ...baseInitialState,
    metroStations: {
        stations: stationsFirstBlock,
    },
});

export const getBaseInitialStateWithTwoBlocks = () => ({
    ...baseInitialState,
    metroStations: {
        stations: stationsFirstBlock.concat(stationsSecondBlock),
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
