/**
 * @jest-environment node
 */

import type { DescriptHttpBlock } from 'descript';
import type { RtbS2SWidgetSettingsParams } from './widgetSettings';

import de from 'descript';
import nock from 'nock';

import getWidgetSettings from './widgetSettings';

let params: RtbS2SWidgetSettingsParams;
let baseBlock: DescriptHttpBlock<unknown, unknown, unknown>;
beforeEach(() => {
    baseBlock = de.http({
        block: {
            protocol: 'https:',
            hostname: 'an.yandex.ru',
        },
    });

    params = {
        'imp-id': '123-123',
    };
});

it('должен сделать правильный запрос', () => {
    const scope = nock('https://an.yandex.ru')
        .get('/widget_settings?charset=utf-8&imp-id=123-123')
        .reply(200, { status: 'OK' }, {
            'content-type': 'application/json',
        });

    const block = getWidgetSettings(baseBlock);
    return de.run(block, { params })
        .then(() => {
            expect(scope.isDone()).toBe(true);
        });
});

describe('обработка ответа', () => {
    it('должен считать 404 за ошибку', async() => {
        nock('https://an.yandex.ru')
            .get('/widget_settings?charset=utf-8&imp-id=123-123')
            .reply(404);

        const block = getWidgetSettings(baseBlock);
        await expect(
            de.run(block, { params }),
        ).rejects.toMatchObject({
            error: { id: 'HTTP_404', status_code: 404 },
        });
    });

    it('должен считать 500 за ошибку и делать ретрай', async() => {
        nock('https://an.yandex.ru')
            .get('/widget_settings?charset=utf-8&imp-id=123-123')
            .times(2)
            .reply(504);

        const block = getWidgetSettings(baseBlock);
        await expect(
            de.run(block, { params }),
        ).rejects.toMatchObject({
            error: { id: 'HTTP_504', status_code: 504 },
        });
    });
});
