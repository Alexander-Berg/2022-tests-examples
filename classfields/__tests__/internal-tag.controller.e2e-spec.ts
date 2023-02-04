import request from 'supertest';
import 'jest-extended';

import { getFixtures } from '../../../tests/get-fixtures';
import { FactoryService } from '../../../tests/factory/factory.service';
import { createTestingApp, ITestingApplication } from '../../../tests/app';

import { fixtures } from './internal-tag.controller.fixtures';

describe('Internal tag controller', () => {
    let testingApp: ITestingApplication;
    let factory: FactoryService;
    let server;

    beforeEach(async () => {
        testingApp = await createTestingApp();
        factory = testingApp.factory;

        const initResponse = await testingApp.initNestApp();

        server = initResponse.server;
    });

    afterEach(async () => {
        await testingApp.close();
    });

    describe('GET /internal/tags', function () {
        it('Возвращает теги', async () => {
            const { TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2, TAG_ATTRIBUTES_3 } = getFixtures(fixtures);

            await factory.createTags(3, [TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2, TAG_ATTRIBUTES_3]);

            const response = await request(server).get('/internal/tags');

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает теги отсортированные по имени', async () => {
            const { TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2, TAG_ATTRIBUTES_3 } = getFixtures(fixtures);

            await factory.createTags(3, [TAG_ATTRIBUTES_2, TAG_ATTRIBUTES_3, TAG_ATTRIBUTES_1]);

            const response = await request(server).get('/internal/tags').query({ orderBy: 'name', orderByAsc: 'true' });

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });

        it('Не дает сортировать по неправильному аттрибуту', async () => {
            const response = await request(server).get('/internal/tags').query({ orderBy: 'invalidAttribute' });

            expect(response.body).toEqual({
                error: 'Некорректный параметр сортировки: "invalidAttribute"',
                status: 400,
            });
            expect(response.statusCode).toBe(400);
        });
    });
});
