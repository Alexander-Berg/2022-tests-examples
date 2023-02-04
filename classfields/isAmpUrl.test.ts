import isAmpUrl from './isAmpUrl';

const TESTS = [
    { url: 'https://auto-ru.cdn.ampproject.org', result: true },
    { url: 'https://auto-ru.cdn.ampproject.org/c/s/example.ru/amp_document.html', result: true },
    { url: 'https://test.auto-ru.cdn.ampproject.org', result: true },
    { url: 'https://auto-ru.cdn.ampproject.org/c/s/example.ru/g?value=Hello%20World', result: true },
    { url: 'https://st.yandexadexchange.net', result: false },
    { url: 'http://x24.ijquery11.com', result: false },
];

TESTS.forEach((testCase) => {
    it(`должен вернуть ${ testCase.result } for ${ JSON.stringify(testCase.url) }`, () => {
        expect(isAmpUrl(testCase.url)).toEqual(testCase.result);
    });
});
