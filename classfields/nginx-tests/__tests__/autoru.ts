import http, { IncomingHttpHeaders, RequestOptions } from 'http';
import https from 'https';

interface Response {
    body: Buffer;
    headers: IncomingHttpHeaders,
    statusCode?: number;
}

let requestOptions: RequestOptions;
beforeEach(() => {
    requestOptions = {
        headers: {
            'user-agent': 'nginx tests bot https://github.com/YandexClassifieds/nginx-tests',
        },
    };
});

describe.each<string>([
    'auto.ru', /*'m.auto.ru',*/
    'test.avto.ru', /*'m.test.avto.ru',*/
])('%s', (domain) => {
    it(`должен сделать редирект http://${ domain } -> https://${ domain }`, async() => {
        const res = await makeRequest(new URL(`http://${ domain }/`), {});
        expect(res).toMatchObject({
            statusCode: 301,
            headers: {
                location: `https://${ domain }/`,
            },
        });
    });

    it(`заголовки https://${ domain }/`, async() => {
        const res = await makeRequest(new URL(`https://${ domain }/`), requestOptions);
        expect(res).toMatchObject({
            statusCode: 200,
            headers: {
                'set-cookie': expect.arrayContaining([
                    expect.stringMatching(/_yasc=/),
                ]),
                'strict-transport-security': expect.stringMatching('max-age=31536000'),
                'x-content-type-options': expect.stringMatching('nosniff'),
                'x-frame-options': expect.stringMatching('SAMEORIGIN'),
                'x-request-id': expect.stringMatching(/^[0-9a-f]{32}$/),
            },
        });
        expect(res.headers).toMatchObject({
            'set-cookie': expect.not.arrayContaining([
                expect.stringMatching(/_ym_uid=/),
                expect.stringMatching(/_ym_d=/),
            ]),
        });
    });

    it(`заголовки https://${ domain }/ реферер от яндекса`, async() => {
        requestOptions.headers!.referer = 'https://metrika.yandex.ru/inpage/visor-proto';
        const res = await makeRequest(new URL(`https://${ domain }/`), requestOptions);
        expect(res).toMatchObject({
            statusCode: 200,
            headers: {
                'set-cookie': expect.arrayContaining([
                    expect.stringMatching(/_yasc=/),
                ]),
                'strict-transport-security': expect.stringMatching('max-age=31536000'),
                'x-request-id': expect.stringMatching(/^[0-9a-f]{32}$/),
            },
        });
        expect(res.headers).toMatchObject({
            'set-cookie': expect.not.arrayContaining([
                expect.stringMatching(/_ym_uid=/),
                expect.stringMatching(/_ym_d=/),
            ]),
        });
        expect(res.headers).not.toHaveProperty('x-content-type-options');
        expect(res.headers).not.toHaveProperty('x-frame-options');
    });

    it(`заголовки https://${ domain }/ при наличии куки _ym_uid`, async() => {
        requestOptions.headers!.cookie = '_ym_uid=1634316784955205689';
        const res = await makeRequest(new URL(`https://${ domain }/`), requestOptions);
        expect(res).toMatchObject({
            statusCode: 200,
            headers: {
                'set-cookie': expect.arrayContaining([
                    expect.stringMatching(/_yasc=/),
                    '_ym_uid=1634316784955205689;Max-Age=31536000;Secure;Path=/;Domain=auto.ru',
                    expect.stringMatching(/_ym_d=\d+;Max-Age=31536000;Secure;Path=\/;Domain=auto\.ru/),
                ]),
                'strict-transport-security': expect.stringMatching('max-age=31536000'),
                'x-content-type-options': expect.stringMatching('nosniff'),
                'x-frame-options': expect.stringMatching('SAMEORIGIN'),
                'x-request-id': expect.stringMatching(/^[0-9a-f]{32}$/),
            },
        });
    });
});

describe.each<string>([
    'm.auto.ru',
    'm.test.avto.ru',
])('%s', (domain) => {
    it(`должен сделать редирект https://${ domain } -> https://${ domain.replace(/^m\./, '') }`, async() => {
        const res = await makeRequest(new URL(`https://${ domain }/`), {});
        expect(res).toMatchObject({
            statusCode: 302,
            headers: {
                location: `https://${ domain.replace(/^m\./, '') }/`,
            },
        });
    });
});

describe.each<string>([
    'apiauto.ru',
    'autoru-api.test.vertis.yandex.net',
])('%s', (domain) => {
    it(`должен сделать редирект http://${ domain } -> https://${ domain }`, async() => {
        const res = await makeRequest(new URL(`http://${ domain }/`), {});
        expect(res).toMatchObject({
            statusCode: 301,
            headers: {
                location: `https://${ domain }/`,
            },
        });
    });

    it(`заголовки https://${ domain }/`, async() => {
        const res = await makeRequest(new URL(`https://${ domain }/`), requestOptions);
        expect(res).toMatchObject({
            headers: {
                'set-cookie': expect.arrayContaining([
                    expect.stringMatching(/_yasc=/),
                ]),
                'strict-transport-security': expect.stringMatching('max-age=31536000'),
                'x-request-id': expect.stringMatching(/^[0-9a-f]{32}$/),
            },
        });
    });
});

function makeRequest(url: URL, options: RequestOptions): Promise<Response> {
    return new Promise((resolve, reject) => {
        const method = url.protocol === 'https:' ? https.get : http.get;
        const req = method(url, options, (res) => {
            const packets: Array<Buffer> = [];

            res.on('data', (chunk) => {
                packets.push(chunk);
            });

            res.on('end', () => {
                resolve({
                    body: Buffer.concat(packets),
                    headers: res.headers,
                    statusCode: res.statusCode,
                });
            });
        });

        req.once('error', (e) => {
            reject(e);
        });

        req.end();
    });
}
