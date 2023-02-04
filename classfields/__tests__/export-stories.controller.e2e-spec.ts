jest.mock('../../../../configs/defaults');
const originalConfig = jest.requireActual('../../../../configs/defaults');

import request from 'supertest';
import 'jest-extended';
import { describe } from '@jest/globals';
import moment from 'moment';

import { LayerEnum, TypeEnum } from 'internal-core/types/export-story';

import { createTestingApp, ITestingApplication } from '../../../tests/app';
import { FactoryService } from '../../../tests/factory/factory.service';
import { getFixtures } from '../../../tests/get-fixtures';
import config from '../../../../configs/defaults';

import { fixtures } from './export-stories.controller.fixtures';

describe('Export stories controller', () => {
    let testingApp: ITestingApplication;
    let factory: FactoryService;
    let server;

    const createStories = async (storiesAttributes, storyPageAttributes) => {
        const stories = await factory.createStories(storiesAttributes.length, storiesAttributes);

        for (const story of stories) {
            await factory.createStoryPages(
                storyPageAttributes.length,
                storyPageAttributes.map(storyPage => ({ ...storyPage, storyId: story.id }))
            );
        }

        return stories;
    };

    beforeEach(async () => {
        config.environment = originalConfig.environment;
        testingApp = await createTestingApp();
        factory = testingApp.factory;

        const initResponse = await testingApp.initNestApp();

        server = initResponse.server;
    });

    afterEach(async () => {
        await testingApp.close();
    });

    describe('GET /internal/export-stories/:layer/:type', () => {
        it('Возвращает список экспортов, если они есть для переданных layer и enum', async () => {
            config.environment = 'production';
            const TOTAL_COUNT = 10;
            const TEST_COUNT = 6;
            const PROD_COUNT = TOTAL_COUNT - TEST_COUNT;
            const { STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES } = getFixtures(fixtures);
            const stories = await createStories(STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES);

            await factory.createExportStories(
                TEST_COUNT,
                stories.slice(0, TEST_COUNT).map((story, index) => ({
                    storyId: story.id,
                    layer: LayerEnum.test,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            await factory.createExportStories(
                PROD_COUNT,
                stories.slice(TEST_COUNT, TOTAL_COUNT).map((story, index) => ({
                    storyId: story.id,
                    layer: LayerEnum.prod,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            const responseForTest = await request(server)
                .get(`/internal/export-stories/${LayerEnum.test}/${TypeEnum.default}`)
                .send();

            const resultForProd = await request(server)
                .get(`/internal/export-stories/${LayerEnum.prod}/${TypeEnum.default}`)
                .send();

            expect(responseForTest.body.exportStories).toHaveLength(TEST_COUNT);
            expect(responseForTest.body).toMatchSnapshot();

            expect(resultForProd.body.exportStories).toHaveLength(PROD_COUNT);
            expect(resultForProd.body).toMatchSnapshot();
        });

        it('Возвращает пустой список историй, если их нет для переданных layer и enum', async () => {
            config.environment = 'production';
            const { STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES } = getFixtures(fixtures);
            const stories = await createStories(STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES);

            await factory.createExportStories(
                10,
                stories.map((story, index) => ({
                    storyId: story.id,
                    layer: LayerEnum.prod,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            const result = await request(server)
                .get(`/internal/export-stories/${LayerEnum.test}/${TypeEnum.default}`)
                .send();

            expect(result.body).toEqual({ exportStories: [] });
        });

        it('Возвращает ошибку, если в тестовом окружении layer === prod', async () => {
            const { STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES } = getFixtures(fixtures);
            const stories = await createStories(STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES);

            await factory.createExportStories(
                10,
                stories.map((story, index) => ({
                    storyId: story.id,
                    layer: LayerEnum.prod,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            const result = await request(server)
                .get(`/internal/export-stories/${LayerEnum.prod}/${TypeEnum.default}`)
                .send();

            expect(result.body).toEqual({ status: 400, error: 'НЕ в продовом окружении layer может быть только test' });
        });
    });

    describe('POST /internal/export-stories/:layer/:type', () => {
        it('Обновляет список историй, если передано 30 или больше историй', async () => {
            const { STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES } = getFixtures(fixtures);
            const stories = await createStories(
                STORIES_ATTRIBUTES.map((story, index) => ({
                    ...story,
                    published: true,
                    publishedAt: moment({
                        year: 2022,
                        month: 6,
                        day: 10,
                        hour: 0,
                        minute: index,
                        second: 0,
                        millisecond: 0,
                    }).format('YYYY-MM-DD HH:mm:ss'),
                })),
                STORY_PAGES_ATTRIBUTES
            );

            const storyIds = stories.map(story => story.id);

            const storyIdsToAdd = storyIds.slice(0, 30);

            const result = await request(server)
                .post(`/internal/export-stories/${LayerEnum.test}/${TypeEnum.default}`)
                .send({
                    storyIds: storyIdsToAdd,
                });

            const resultStoryIds = result.body.exportStories.map(item => item.storyId);

            expect(resultStoryIds).toEqual(storyIdsToAdd);
        });

        it('Обновляет список историй, дополняя до 30 из последних опубликованных', async () => {
            const { STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES } = getFixtures(fixtures);
            const stories = await createStories(
                STORIES_ATTRIBUTES.map((story, index) => ({
                    ...story,
                    published: true,
                    publishedAt: moment({
                        year: 2022,
                        month: 6,
                        day: 10,
                        hour: 0,
                        minute: index,
                        second: 0,
                        millisecond: 0,
                    }).format('YYYY-MM-DD HH:mm:ss'),
                })),
                STORY_PAGES_ATTRIBUTES
            );

            const storyIds = stories.map(story => story.id);

            const storyIdsToAdd = storyIds.slice(0, 2);

            const lastPublishedStoryIds = stories
                .slice()
                .sort((a, b) => moment(b.publishedAt).diff(a.publishedAt))
                .slice(0, 30 - storyIdsToAdd.length)
                .map(story => story.id);

            const result = await request(server)
                .post(`/internal/export-stories/${LayerEnum.test}/${TypeEnum.default}`)
                .send({
                    storyIds: storyIdsToAdd,
                });

            const resultStoryIds = result.body.exportStories.map(item => item.storyId);

            expect(resultStoryIds).toHaveLength(30);
            expect(resultStoryIds).toEqual(storyIdsToAdd.concat(lastPublishedStoryIds));
        });

        it('Возвращает ошибку, если в тестовом окружении layer === prod', async () => {
            const { STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES } = getFixtures(fixtures);
            const stories = await createStories(
                STORIES_ATTRIBUTES.map((story, index) => ({
                    ...story,
                    published: true,
                    publishedAt: moment({
                        year: 2022,
                        month: 6,
                        day: 10,
                        hour: 0,
                        minute: index,
                        second: 0,
                        millisecond: 0,
                    }).format('YYYY-MM-DD HH:mm:ss'),
                })),
                STORY_PAGES_ATTRIBUTES
            );

            const storyIds = stories.map(story => story.id);

            const storyIdsToAdd = storyIds.slice(0, 2);

            const result = await request(server)
                .post(`/internal/export-stories/${LayerEnum.prod}/${TypeEnum.default}`)
                .send({
                    storyIds: storyIdsToAdd,
                });

            expect(result.body).toEqual({ status: 400, error: 'НЕ в продовом окружении layer может быть только test' });
        });
    });

    describe('POST /internal/export-stories/:layer/:type/add-story-to-position', () => {
        it('Добавляет историю на указанную позицию в списке и сдвигает остальные', async () => {
            const { STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES } = getFixtures(fixtures);
            const stories = await createStories(
                STORIES_ATTRIBUTES.map((story, index) => ({
                    ...story,
                    published: true,
                    publishedAt: moment({
                        year: 2022,
                        month: 6,
                        day: 10,
                        hour: 0,
                        minute: index,
                        second: 0,
                        millisecond: 0,
                    }).format('YYYY-MM-DD HH:mm:ss'),
                })),
                STORY_PAGES_ATTRIBUTES
            );

            const storyIds = stories.map(story => story.id);

            const storyIdsToAdd = storyIds.slice(0, 4);

            const exportedStories = await factory.createExportStories(
                storyIdsToAdd.length,
                storyIdsToAdd.map((storyId, index) => ({
                    storyId,
                    layer: LayerEnum.test,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            const positionToInsert = 2;

            const previousExportStoryAtPositionId = exportedStories.find(
                exportStory => exportStory.position === positionToInsert
            )?.id;

            const storyToInsert = stories[5];

            const result = await request(server)
                .post(`/internal/export-stories/${LayerEnum.test}/${TypeEnum.default}/add-story-to-position`)
                .send({
                    position: positionToInsert,
                    storyId: storyToInsert?.id || 0,
                });

            const updatedExportedStories = result.body.exportStories;

            const newStoryAtPosition = updatedExportedStories.find(
                exportStory => exportStory.position === positionToInsert
            );
            const previousExportStoryAtPosition = updatedExportedStories.find(
                exportStory => exportStory.id === previousExportStoryAtPositionId
            );

            expect(previousExportStoryAtPosition).not.toEqual(newStoryAtPosition);
            expect(newStoryAtPosition?.position).toBe(positionToInsert);
            expect(previousExportStoryAtPosition?.position).toBe(positionToInsert + 1);
        });

        it('Возвращает ошибку, если в тестовом окружении layer === prod', async () => {
            const { STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES } = getFixtures(fixtures);
            const stories = await createStories(STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES);

            await factory.createExportStories(
                5,
                stories.map((story, index) => ({
                    storyId: story.id,
                    layer: LayerEnum.prod,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            const positionToInsert = 2;

            const storyToInsert = stories[5];

            const result = await request(server)
                .post(`/internal/export-stories/${LayerEnum.prod}/${TypeEnum.default}`)
                .send({
                    position: positionToInsert,
                    storyId: storyToInsert?.id || 0,
                });

            expect(result.body).toEqual({ status: 400, error: 'НЕ в продовом окружении layer может быть только test' });
        });
    });

    describe('POST /internal/export-stories/:layer/:type/move', () => {
        it('Перемещает экспорт на новую позицию', async () => {
            const { STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES } = getFixtures(fixtures);
            const stories = await createStories(
                STORIES_ATTRIBUTES.map((story, index) => ({
                    ...story,
                    published: true,
                    publishedAt: moment({
                        year: 2022,
                        month: 6,
                        day: 10,
                        hour: 0,
                        minute: index,
                        second: 0,
                        millisecond: 0,
                    }).format('YYYY-MM-DD HH:mm:ss'),
                })),
                STORY_PAGES_ATTRIBUTES
            );

            const storyIds = stories.map(story => story.id);

            const storyIdsToAdd = storyIds.slice(0, 30);

            const exportedStories = await factory.createExportStories(
                storyIdsToAdd.length,
                storyIdsToAdd.map((storyId, index) => ({
                    storyId,
                    layer: LayerEnum.test,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            const previousPositions = exportedStories.map(({ position }) => position);

            const exportStoryToMove = exportedStories[10];

            if (!exportStoryToMove) {
                throw 'Something bad happened';
            }
            const oldPosition = exportStoryToMove.position;
            const newPosition = oldPosition + 5;

            const result = await request(server)
                .post(`/internal/export-stories/${LayerEnum.test}/${TypeEnum.default}/move`)
                .send({
                    oldPosition,
                    newPosition,
                });

            expect(result.body.exportStories.length).toBe(exportedStories.length);
            const newPositions = result.body.exportStories.map(({ position }) => position);

            expect(newPositions).toEqual(previousPositions);

            const movedStory = result.body.exportStories.find(({ id }) => id === exportStoryToMove.id);

            expect(movedStory?.position).toBe(newPosition);
        });

        it('Возвращает ошибку, если в тестовом окружении layer === prod', async () => {
            const { STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES } = getFixtures(fixtures);
            const stories = await createStories(STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES);

            await factory.createExportStories(
                5,
                stories.map((story, index) => ({
                    storyId: story.id,
                    layer: LayerEnum.prod,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            const result = await request(server)
                .post(`/internal/export-stories/${LayerEnum.prod}/${TypeEnum.default}/move`)
                .send({
                    oldPosition: 1,
                    newPosition: 10,
                });

            expect(result.body).toEqual({ status: 400, error: 'НЕ в продовом окружении layer может быть только test' });
        });
    });

    describe('DELETE /internal/export-stories/:layer/:type/:id', () => {
        it('Удаляет экспорт и сдвигает остальные', async () => {
            const { STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES } = getFixtures(fixtures);
            const stories = await createStories(
                STORIES_ATTRIBUTES.map((story, index) => ({
                    ...story,
                    published: true,
                    publishedAt: moment({
                        year: 2022,
                        month: 6,
                        day: 10,
                        hour: 0,
                        minute: index,
                        second: 0,
                        millisecond: 0,
                    }).format('YYYY-MM-DD HH:mm:ss'),
                })),
                STORY_PAGES_ATTRIBUTES
            );

            const storyIds = stories.map(story => story.id);

            const storyIdsToAdd = storyIds.slice(0, 30);

            const exportedStories = await factory.createExportStories(
                storyIdsToAdd.length,
                storyIdsToAdd.map((storyId, index) => ({
                    storyId,
                    layer: LayerEnum.test,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            const deletedExportStory = exportedStories[0];

            if (!deletedExportStory) {
                throw 'Something bad happened';
            }

            const result = await request(server)
                .delete(`/internal/export-stories/${LayerEnum.test}/${TypeEnum.default}/${deletedExportStory.id}`)
                .send();

            expect(result.body.exportStories.length).toBe(exportedStories.length);
            expect(
                result.body.exportStories.find(exportStory => exportStory.id === deletedExportStory?.id)
            ).toBeUndefined();
            expect(
                result.body.exportStories.find(exportStory => exportStory.position === deletedExportStory?.position)
            ).toBeDefined();
        });

        it('Возвращает ошибку, если в тестовом окружении layer === prod', async () => {
            const { STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES } = getFixtures(fixtures);
            const stories = await createStories(STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES);

            const exportedStories = await factory.createExportStories(
                5,
                stories.map((story, index) => ({
                    storyId: story.id,
                    layer: LayerEnum.prod,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            const deletedExportStory = exportedStories[0];

            if (!deletedExportStory) {
                throw 'Something bad happened';
            }

            const result = await request(server)
                .delete(`/internal/export-stories/${LayerEnum.prod}/${TypeEnum.default}/${deletedExportStory.id}`)
                .send();

            expect(result.body).toEqual({ status: 400, error: 'НЕ в продовом окружении layer может быть только test' });
        });
    });
});
