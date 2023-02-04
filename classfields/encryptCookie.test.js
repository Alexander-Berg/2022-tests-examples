const encryptCookie = require('auto-core/models/cookieSync/helpers/encryptCookie');

it('возвращает шифрованную куку, если она передана', () => {
    expect(encryptCookie('some value', Buffer.alloc(8, '5'))).toEqual('pQW95jGwurf8fD2V5lc=');
});

it('ничего не возвращает, если ничего не передано', () => {
    expect(encryptCookie()).toBeUndefined();
});
