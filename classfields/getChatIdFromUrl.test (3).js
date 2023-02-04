const getChatIdFromUrl = require('./getChatIdFromUrl');


const TESTS = [
    { url: 'http://auto.ru/chat/CHAT_ID', result: 'CHAT_ID' },
    { url: 'http://auto.ru/cars/chat/CHAT_ID', result: 'CHAT_ID' },
    { url: 'http://auto.ru/cars/chat/?chat_id=CHAT_ID', result: 'CHAT_ID' },
    { url: 'http://auto.ru/cars/all/?chat&chat_id=CHAT_ID', result: 'CHAT_ID' },

    { url: undefined, result: null },
    { url: 'bla', result: null },
    { url: 'http://auto.ru', result: null },
    { url: 'http://auto.ru/chat/', result: null },
    { url: 'http://auto.ru/cars/chat', result: null },
    { url: 'http://auto.ru/cars/all/?foo=bar', result: null },
];

TESTS.forEach((testCase) => {
    it(`должен вернуть ${ testCase.result } для ${ testCase.url }`, () => {
        expect(getChatIdFromUrl(testCase.url)).toEqual(testCase.result);
    });
});
