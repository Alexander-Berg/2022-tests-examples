export const suggestions = [
    {
        url: '/baz',
        text: 'В новостройке',
        offersCount: 1505,
    },
    {
        url: '/bar',
        text: 'Проверенные в ЕГРН',
        offersCount: 65,
    },
    {
        url: '/foo',
        text: 'Не первый этаж',
        offersCount: 1999,
    },
] as const;

export const initialState = {
    user: {
        crc: '123crc',
    },
    offersSearchPage: {
        searchResults: {
            queryId: '123queryId',
        },
    },
} as const;
