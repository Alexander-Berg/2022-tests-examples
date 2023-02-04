/**
 * @jest-environment node
 */

import type { DescriptHttpBlock } from 'descript';
import type { RtbS2SMetaParams } from './meta';

import de from 'descript';
import nock from 'nock';

import getMeta from './meta';

let params: RtbS2SMetaParams;
let baseBlock: DescriptHttpBlock<unknown, unknown, unknown>;
beforeEach(() => {
    baseBlock = de.http({
        block: {
            protocol: 'https:',
            hostname: 'an.yandex.ru',
        },
    });

    params = {
        awaps_section: '29337',
        clientIp: 'clientIp',
        'imp-id': '1',
        headers: {
            'accept-language': 'ru-RU',
            cookie: 'cookie=value',
            'user-agent': 'ua jest',
            'x-forwarded-for': 'x-forwarded-for',
        },
        pageId: '148383',
        'partner-stat-id': '200',
        referer: 'https://auto.ru/referer',
        url: 'https://auto.ru/current_url',
    };
});

it('должен сделать правильный запрос и подставить заголовки', () => {
    const scope = nock('https://an.yandex.ru', {
        reqheaders: {
            accept: 'application/json',
            'accept-language': 'ru-RU',
            cookie: 'cookie=value',
            referer: 'https://auto.ru/current_url',
            'user-agent': 'ua jest',
            'x-forwarded-for': 'x-forwarded-for',
            'x-forwarded-proto': 'https',
            'x-real-ip': 'clientIp',
            'x-yabs-rereferer': 'https://auto.ru/referer',
        },
    })
        // eslint-disable-next-line max-len
        .get('/meta/148383?awaps_section=29337&charset=utf-8&callback=json&imp-id=1&page-ref=https%3A%2F%2Fauto.ru%2Freferer&partner-stat-id=200&redir-setuniq=1&server-side=1&target-ref=https%3A%2F%2Fauto.ru%2Fcurrent_url')
        .reply(200, { status: 'OK' }, {
            'content-type': 'application/json',
        });

    const block = getMeta(baseBlock);
    return de.run(block, { params })
        .then(() => {
            expect(scope.isDone()).toBe(true);
        });
});

describe('обработка ответа', () => {
    it('должен считать 404 за ошибку', async() => {
        nock('https://an.yandex.ru')
            .get('/meta/148383')
            .query(true)
            .reply(404);

        const block = getMeta(baseBlock);
        await expect(
            de.run(block, { params }),
        ).rejects.toMatchObject({
            error: { id: 'HTTP_404', status_code: 404 },
        });
    });

    it('должен считать 500 за ошибку', async() => {
        nock('https://an.yandex.ru')
            .get('/meta/148383')
            .query(true)
            .reply(504);

        const block = getMeta(baseBlock);
        await expect(
            de.run(block, { params }),
        ).rejects.toMatchObject({
            error: { id: 'HTTP_504', status_code: 504 },
        });
    });
});
