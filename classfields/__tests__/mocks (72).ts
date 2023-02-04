export const villageInfo = {
    villageId: '1881483',
    villageFullName: 'Коттеджный посёлок «Ломоносовская усадьба»',
    villageName: 'Ломоносовская усадьба',
    deliveryDates: [
        { phaseName: '1 очередь', phaseIndex: 1, status: 'HAND_OVER', year: 2018, quarter: 4, finished: true },
        { phaseName: '2 очередь', phaseIndex: 2, status: 'HAND_OVER', year: 2019, quarter: 1, finished: true },
        { phaseName: '3 очередь', phaseIndex: 3, status: 'HAND_OVER', year: 2019, quarter: 4, finished: true },
        { phaseName: '4 очередь', phaseIndex: 4, status: 'HAND_OVER', year: 2020, quarter: 4, finished: true },
        { phaseName: '5 очередь', phaseIndex: 5, status: 'UNFINISHED', year: 2021, quarter: 4, finished: false },
    ],
    villageFeatures: {
        villageClass: 'COMFORT' as const,
    },
    developer: {
        id: 618135,
        name: 'Ломоносовская усадьба',
    },
    populatedRgid: '741965',
};

export const initialState = {
    user: {
        favorites: [],
        favoritesMap: {},
    },
    page: {
        name: 'map',
    },
};
