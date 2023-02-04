import request from 'supertest';
import 'jest-extended';

import { getFixtures } from '../../../tests/get-fixtures';
import { FactoryService } from '../../../tests/factory/factory.service';
import { createTestingApp, ITestingApplication } from '../../../tests/app';

import { fixtures } from './internal-story.controller.fixtures';

describe('Internal story controller', () => {
    let testingApp: ITestingApplication;
    let factory: FactoryService;
    let server;

    beforeEach(async () => {
        Date.now = jest.fn(() => new Date('2022-04-07T10:20:30Z').getTime());

        testingApp = await createTestingApp();
        factory = testingApp.factory;

        const initResponse = await testingApp.initNestApp();

        server = initResponse.server;
    });

    afterEach(async () => {
        await testingApp.close();
    });

    describe('GET /internal/stories', function () {
        it('Возвращает отфильтрованные и отсортированные сторис с правильным размером страницы', async () => {
            const {
                STORY_ATTRIBUTES_1,
                STORY_ATTRIBUTES_2,
                STORY_ATTRIBUTES_3,
                STORY_ATTRIBUTES_4,
                STORY_ATTRIBUTES_5,
            } = getFixtures(fixtures);

            await factory.createStories(6, [
                STORY_ATTRIBUTES_1,
                STORY_ATTRIBUTES_2,
                STORY_ATTRIBUTES_3,
                STORY_ATTRIBUTES_4,
                STORY_ATTRIBUTES_5,
            ]);
            const response = await request(server)
                .get('/internal/stories')
                .query({ published: true, pageSize: 3, orderBy: 'publishedAt', orderByAsc: false });

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });
    });

    describe('POST /internal/stories', function () {
        it('Создает и возвращает новый сторис', async () => {
            const { STORY_ATTRIBUTES, STORY_PAGE_ATTRIBUTES } = getFixtures(fixtures);
            const requestData = { ...STORY_ATTRIBUTES, pages: [STORY_PAGE_ATTRIBUTES] };

            const response = await request(server).post('/internal/stories').send(requestData);

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });
    });

    describe('PUT /internal/stories/:id', function () {
        it('Возвращает ошибку и статус 404, если сторис не найден', async () => {
            const NOT_EXISTING_UUID = 'c0efa04e-2072-4aeb-bd90-064aaa9d5aa7';

            const response = await request(server).put(`/internal/stories/${NOT_EXISTING_UUID}`).send({});

            expect(response.body).toEqual({
                status: 404,
                error: 'История не найдена',
            });
            expect(response.statusCode).toBe(404);
        });

        it('Обновляет сторис по id и возвращает его', async () => {
            const STORY_UUID = 'c0efa04e-2072-4aeb-bd90-064aaa9d5aa7';
            const story = await factory.createStory({ uuid: STORY_UUID });
            const { STORY_ATTRIBUTES } = getFixtures(fixtures);

            const response = await request(server).put(`/internal/stories/${story.id}`).send(STORY_ATTRIBUTES);

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });

        it('Обновляет сторис по uuid и возвращает его', async () => {
            const STORY_UUID = 'c0efa04e-2072-4aeb-bd90-064aaa9d5aa7';
            const story = await factory.createStory({ uuid: STORY_UUID });
            const { STORY_ATTRIBUTES } = getFixtures(fixtures);

            const response = await request(server).put(`/internal/stories/${story.uuid}`).send(STORY_ATTRIBUTES);

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });
    });

    describe('DELETE /internal/stories/:id', function () {
        it('Возвращает ошибку и статус 404, если сторис не найден', async () => {
            const NOT_EXISTING_UUID = 'c0efa04e-2072-4aeb-bd90-064aaa9d5aa7';

            const response = await request(server).delete(`/internal/stories/${NOT_EXISTING_UUID}`).send();

            expect(response.body).toEqual({
                status: 404,
                error: 'История не найдена',
            });
            expect(response.statusCode).toBe(404);
        });

        it('Удаляет сторис по id', async () => {
            const story = await factory.createStory();

            const response = await request(server).delete(`/internal/stories/${story.id}`).send();

            expect(response.body).toEqual({ status: 'ok' });
            expect(response.statusCode).toBe(200);
        });

        it('Удаляет сторис по uuid', async () => {
            const story = await factory.createStory();

            const response = await request(server).delete(`/internal/stories/${story.uuid}`).send();

            expect(response.body).toEqual({ status: 'ok' });
            expect(response.statusCode).toBe(200);
        });
    });

    describe('POST /internal/stories/:id/publish', function () {
        it('Возвращает ошибку и статус 404, если сторис не найден', async () => {
            const NOT_EXISTING_UUID = 'c0efa04e-2072-4aeb-bd90-064aaa9d5aa7';

            const response = await request(server).post(`/internal/stories/${NOT_EXISTING_UUID}/publish`).send();

            expect(response.body).toEqual({
                status: 404,
                error: 'История не найдена',
            });
            expect(response.statusCode).toBe(404);
        });

        it('Публикует сторис по id', async () => {
            const { STORY_ATTRIBUTES } = getFixtures(fixtures);
            const story = await factory.createStory(STORY_ATTRIBUTES);

            const response = await request(server).post(`/internal/stories/${story.id}/publish`).send();

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });

        it('Публикует сторис по uuid', async () => {
            const { STORY_ATTRIBUTES } = getFixtures(fixtures);
            const story = await factory.createStory(STORY_ATTRIBUTES);

            const response = await request(server).post(`/internal/stories/${story.uuid}/publish`).send();

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });
    });

    describe('POST /internal/stories/:id/unpublish', function () {
        it('Возвращает ошибку и статус 404, если сторис не найден', async () => {
            const NOT_EXISTING_UUID = 'c0efa04e-2072-4aeb-bd90-064aaa9d5aa7';

            const response = await request(server).post(`/internal/stories/${NOT_EXISTING_UUID}/unpublish`);

            expect(response.body).toEqual({
                status: 404,
                error: 'История не найдена',
            });
            expect(response.statusCode).toBe(404);
        });

        it('Снимает с публикации сторис по id', async () => {
            const { STORY_ATTRIBUTES } = getFixtures(fixtures);
            const story = await factory.createStory(STORY_ATTRIBUTES);

            const response = await request(server).post(`/internal/stories/${story.id}/unpublish`);

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });

        it('Снимает с публикации сторис по uuid', async () => {
            const { STORY_ATTRIBUTES } = getFixtures(fixtures);
            const story = await factory.createStory(STORY_ATTRIBUTES);

            const response = await request(server).post(`/internal/stories/${story.uuid}/unpublish`);

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });
    });
});
