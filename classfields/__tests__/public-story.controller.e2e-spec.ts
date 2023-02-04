import request from 'supertest';
import 'jest-extended';

import { getFixtures } from '../../../tests/get-fixtures';
import { FactoryService } from '../../../tests/factory/factory.service';
import { createTestingApp, ITestingApplication } from '../../../tests/app';

import { fixtures } from './public-story.controller.fixtures';

describe('Public story controller', () => {
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

    describe('GET /stories', function () {
        it('Возвращает опубликованные трансформированные сторис', async () => {
            const { STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3 } = getFixtures(fixtures);

            await factory.createStories(3, [STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3]);

            const response = await request(server).get('/stories');

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает опубликованные сторис, отсортированные по убыванию даты публикации', async () => {
            const { STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3 } = getFixtures(fixtures);

            await factory.createStories(3, [STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3]);

            const response = await request(server).get('/stories').query({ orderBy: 'publishedAt' });

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });

        it('Может отдавать сторис с указанным лимитом и страницей', async () => {
            const { STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3 } = getFixtures(fixtures);

            await factory.createStories(3, [STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3]);

            const response = await request(server).get('/stories').query({ pageNumber: 1, pageSize: 2 });

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });

        it('Возвращает ошибку и статус 400, если порядок сортировки некорректен', async () => {
            const { STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3 } = getFixtures(fixtures);

            await factory.createStories(3, [STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3]);

            const response = await request(server).get('/stories').query({ orderBy: 'unknown' });

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(400);
        });
    });

    describe('GET /stories/:urlPart', function () {
        it('Возвращает ошибку и статус 404, если сторис не найден', async () => {
            const NOT_EXISTING_URLPART = 'non-existing';

            const response = await request(server).get(`/stories/${NOT_EXISTING_URLPART}`);

            expect(response.body).toEqual({
                status: 404,
                error: 'История не найдена',
            });
            expect(response.statusCode).toBe(404);
        });

        it('Возвращает ошибку и статус 404, если сторис не опубликован', async () => {
            const { STORY_ATTRIBUTES, STORY_PAGE_ATTRIBUTES } = getFixtures(fixtures);
            const story = await factory.createStory({ ...STORY_ATTRIBUTES });

            await factory.createStoryPage({ ...STORY_PAGE_ATTRIBUTES, storyId: story.id });

            const response = await request(server).get(`/stories/${story.urlPart}`);

            expect(response.body).toEqual({
                status: 404,
                error: 'История не найдена',
            });
            expect(response.statusCode).toBe(404);
        });

        it('Возвращает опубликованный трансформированный сторис и его страницы по urlPart', async () => {
            const { STORY_ATTRIBUTES, STORY_PAGE_ATTRIBUTES } = getFixtures(fixtures);
            const story = await factory.createStory({ ...STORY_ATTRIBUTES });

            await factory.createStoryPage({ ...STORY_PAGE_ATTRIBUTES, storyId: story.id });

            const response = await request(server).get(`/stories/${story.urlPart}`);

            expect(response.body).toMatchSnapshot();
            expect(response.statusCode).toBe(200);
        });
    });
});
