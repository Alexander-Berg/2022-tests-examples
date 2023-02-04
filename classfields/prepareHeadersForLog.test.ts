import { prepareHeadersForLog } from './prepareHeadersForLog';

it('должен замаскировать общие заголовки', () => {
    expect(
        prepareHeadersForLog({
            'x-authorization': 'Vertis web-ca0fdac1234567890',
            'x-session-id': '67768188|1631212367880.7776000.1234567890',
            'x-ya-user-ticket-vertis': '123456789012345678901234567890abcdefg',
        }),
    ).toEqual({
        'x-authorization': 'Vertis web-ca0XXXXXXXXXXXXXX',
        'x-session-id': '67768188|16312123678XXXXXXXXXXXXXXXXXXXXX',
        'x-ya-user-ticket-vertis': '1234567890***',
    });
});

it('должен замаскировать x-yandex-exp* заголовки uaas', () => {
    expect(
        prepareHeadersForLog({
            'x-yandex-expboxes': '412886,0,5,412886,0,5,412886,0,5',
            'x-yandex-expflags': '1234567890abcdefg',
        }),
    ).toEqual({
        'x-yandex-expboxes': '412886,0,5***',
        'x-yandex-expflags': '1234567890***',
    });
});

it('должен замаскировать x-aab-partnertoken заголовок от AntiAdblock', () => {
    expect(
        prepareHeadersForLog({
            'x-aab-partnertoken': 'eyJhbGciOiJIUzI1NiIsIeyJhbGciOiJIUzI1NiIsIeyJhbGciOiJIUzI1NiIsIeyJhbGciOiJIUzI1NiIsI',
            'x-aab-requestid': 'd4ee2c5706d6d2fad04e3ef2f07e83d5',
        }),
    ).toEqual({
        'x-aab-partnertoken': 'eyJhbGciOi***',
        'x-aab-requestid': 'd4ee2c5706d6d2fad04e3ef2f07e83d5',
    });
});

it('не должен замаскировать заголовки x-yandex-ja3', () => {
    expect(
        prepareHeadersForLog({
            'x-yandex-ja3': '772,4865-4,772,4865-4,772,4865-4,772,4865-4,772,4865-4',
            'x-yandex-p0f': '1:2:3:4',
            'x-yandex-antirobot-degradation': '1',
        }),
    ).toEqual({
        'x-yandex-ja3': '772,4865-4,772,4865-4,772,4865-4,772,4865-4,772,4865-4',
        'x-yandex-p0f': '1:2:3:4',
        'x-yandex-antirobot-degradation': '1',
    });
});
