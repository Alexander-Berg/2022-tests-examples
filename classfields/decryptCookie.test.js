const decryptCookie = require('auto-core/models/cookieSync/helpers/decryptCookie');

it('возвращает расшифрованную куку, если она передана', () => {
    expect(decryptCookie('pQW95jGwurf8fD2V5lc=', Buffer.alloc(8, '5'))).toEqual('some value');
});

it('ничего не возвращает, если ничего не передано', () => {
    expect(decryptCookie()).toBeUndefined();
});
