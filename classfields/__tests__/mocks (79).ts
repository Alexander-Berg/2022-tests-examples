import { stations } from './raw-stations';

const baseInitialState = {
    geo: {
        rgid: 587795,
        locative: 'в Москве',
    },
};

export const getBaseInitialStateFullList = () => ({
    ...baseInitialState,
    railways: {
        stations,
    },
});

export const getBaseInitialStateFiveStations = () => ({
    ...baseInitialState,
    railways: {
        stations: stations.slice(0, 5),
    },
});

export const getBaseInitialStateWithoutStations = () => ({
    ...baseInitialState,
    railways: {
        stations: [],
    },
});
