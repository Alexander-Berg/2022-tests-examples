import { describe } from '@jest/globals';
import moment from 'moment';
import { BadRequestException } from '@nestjs/common';

import { LayerEnum, TypeEnum } from 'internal-core/types/export-story';

import { createTestingApp, ITestingApplication } from '../../../tests/app';
import { FactoryService } from '../../../tests/factory/factory.service';
import { getFixtures } from '../../../tests/get-fixtures';
import { ExportStoriesService, STORIES_EXPORT_LIMIT } from '../export-stories.service';
import { NotFoundException } from '../../../exceptions';

import { fixtures } from './export-stories.service.fixtures';

describe('Export stories service', () => {
    let testingApp: ITestingApplication;
    let factory: FactoryService;
    let exportStoriesService: ExportStoriesService;

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
        Date.now = jest.fn(() => new Date('2022-04-07T10:20:30Z').getTime());

        testingApp = await createTestingApp();

        factory = testingApp.factory;
        exportStoriesService = await testingApp.module.resolve(ExportStoriesService);
    });

    afterEach(async () => {
        await testingApp.close();
    });

    describe('findByLayerAndType', () => {
        it('Возвращает список экспортов, если они есть для переданных layer и enum', async () => {
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

            const resultForTest = await exportStoriesService.findByLayerAndType({
                layer: LayerEnum.test,
                type: TypeEnum.default,
            });

            const resultForProd = await exportStoriesService.findByLayerAndType({
                layer: LayerEnum.prod,
                type: TypeEnum.default,
            });

            expect(resultForTest).toHaveLength(TEST_COUNT);
            expect(resultForTest).toMatchSnapshot();

            expect(resultForProd).toHaveLength(PROD_COUNT);
            expect(resultForProd).toMatchSnapshot();
        });

        it('Возвращает пустой список, если их нет для переданных layer и enum', async () => {
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

            const result = await exportStoriesService.findByLayerAndType({
                layer: LayerEnum.test,
                type: TypeEnum.default,
            });

            expect(result).toHaveLength(0);
        });
    });

    describe('update', () => {
        it('Обновляет список историй, если передано 30 историй', async () => {
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

            const storyIdsToAdd = storyIds.slice(0, STORIES_EXPORT_LIMIT);

            const result = await exportStoriesService.update({
                layer: LayerEnum.test,
                type: TypeEnum.default,
                storyIds: storyIdsToAdd,
            });

            const resultStoryIds = result.map(item => item.storyId);

            expect(resultStoryIds).toEqual(storyIdsToAdd);
        });

        it('Обновляет список историй, обрезая до 30, если передано больше 30 историй', async () => {
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

            const storyIdsToAdd = storyIds.slice(0, STORIES_EXPORT_LIMIT + 10);

            const result = await exportStoriesService.update({
                layer: LayerEnum.test,
                type: TypeEnum.default,
                storyIds: storyIdsToAdd,
            });

            const resultStoryIds = result.map(item => item.storyId);

            expect(resultStoryIds).toEqual(storyIdsToAdd.slice(0, STORIES_EXPORT_LIMIT));
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

            const result = await exportStoriesService.update({
                layer: LayerEnum.test,
                type: TypeEnum.default,
                storyIds: storyIdsToAdd,
            });

            const resultStoryIds = result.map(item => item.storyId);

            expect(resultStoryIds).toHaveLength(STORIES_EXPORT_LIMIT);
            expect(resultStoryIds).toEqual(storyIdsToAdd.concat(lastPublishedStoryIds));
        });

        it('Обновляет список историй только для переданных layer и type', async () => {
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

            const initialStories = storyIds.slice(0, STORIES_EXPORT_LIMIT);
            const storyIdsToAdd = storyIds.slice(STORIES_EXPORT_LIMIT, storyIds.length);

            await factory.createExportStories(
                STORIES_EXPORT_LIMIT,
                initialStories.map((storyId, index) => ({
                    storyId,
                    layer: LayerEnum.prod,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            await factory.createExportStories(
                STORIES_EXPORT_LIMIT,
                initialStories.map((storyId, index) => ({
                    storyId,
                    layer: LayerEnum.test,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            const testExportStoriesInitial = await exportStoriesService.findByLayerAndType({
                layer: LayerEnum.prod,
                type: TypeEnum.default,
            });

            const prodExportStoriesInitial = await exportStoriesService.findByLayerAndType({
                layer: LayerEnum.prod,
                type: TypeEnum.default,
            });

            await exportStoriesService.update({
                layer: LayerEnum.test,
                type: TypeEnum.default,
                storyIds: storyIdsToAdd,
            });

            const prodExportStoriesAfterUpdate = await exportStoriesService.findByLayerAndType({
                layer: LayerEnum.prod,
                type: TypeEnum.default,
            });

            const testExportStoriesAfterUpdate = await exportStoriesService.findByLayerAndType({
                layer: LayerEnum.test,
                type: TypeEnum.default,
            });

            expect(prodExportStoriesAfterUpdate).toEqual(prodExportStoriesInitial);
            expect(testExportStoriesAfterUpdate).not.toEqual(testExportStoriesInitial);
        });
    });

    describe('addStoryToPosition', () => {
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

            const storyIdsToAdd = storyIds.slice(0, STORIES_EXPORT_LIMIT);

            await factory.createExportStories(
                storyIdsToAdd.length,
                storyIdsToAdd.map((storyId, index) => ({
                    storyId,
                    layer: LayerEnum.test,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            await factory.createExportStories(
                storyIdsToAdd.length,
                storyIdsToAdd.map((storyId, index) => ({
                    storyId,
                    layer: LayerEnum.prod,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            const exportedTestStories = await exportStoriesService.findByLayerAndType({
                layer: LayerEnum.test,
                type: TypeEnum.default,
            });

            const exportedProdStories = await exportStoriesService.findByLayerAndType({
                layer: LayerEnum.prod,
                type: TypeEnum.default,
            });

            const positionToInsert = 2;

            const previousExportStoryAtPositionId = exportedTestStories.find(
                exportStory => exportStory.position === positionToInsert
            )?.id;

            const storyToInsert = stories[40];

            await exportStoriesService.addStoryToPosition({
                layer: LayerEnum.test,
                type: TypeEnum.default,
                position: positionToInsert,
                storyId: storyToInsert?.id || 0,
            });

            const prodExportStoriesAfterInsert = await exportStoriesService.findByLayerAndType({
                layer: LayerEnum.prod,
                type: TypeEnum.default,
            });

            expect(exportedProdStories).toEqual(prodExportStoriesAfterInsert);

            const testExportedStoriesAfterInsert = await exportStoriesService.findByLayerAndType({
                layer: LayerEnum.test,
                type: TypeEnum.default,
            });

            expect(testExportedStoriesAfterInsert.length).toEqual(exportedTestStories.length);
            expect(testExportedStoriesAfterInsert).not.toEqual(exportedTestStories);

            const newStoryAtPosition = testExportedStoriesAfterInsert.find(
                exportStory => exportStory.position === positionToInsert
            );
            const previousExportStoryAtPosition = testExportedStoriesAfterInsert.find(
                exportStory => exportStory.id === previousExportStoryAtPositionId
            );

            expect(previousExportStoryAtPosition).not.toEqual(newStoryAtPosition);
            expect(newStoryAtPosition?.position).toBe(positionToInsert);
            expect(previousExportStoryAtPosition?.position).toBe(positionToInsert + 1);
        });

        it('Возвращает ошибку, если история не опубликована', async () => {
            const { STORIES_ATTRIBUTES, STORY_PAGES_ATTRIBUTES } = getFixtures(fixtures);
            const notPublishedIndex = 0;
            const stories = await createStories(
                STORIES_ATTRIBUTES.map((story, index) => ({
                    ...story,
                    published: index !== notPublishedIndex,
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

            const notPublishedStory = stories.find(story => !story.published);

            const storyIdsToAdd = storyIds.slice(0, STORIES_EXPORT_LIMIT);

            await factory.createExportStories(
                storyIdsToAdd.length,
                storyIdsToAdd.map((storyId, index) => ({
                    storyId,
                    layer: LayerEnum.test,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            const positionToInsert = 2;

            const updatedExportedStories = exportStoriesService.addStoryToPosition({
                layer: LayerEnum.test,
                type: TypeEnum.default,
                position: positionToInsert,
                storyId: notPublishedStory?.id || 0,
            });

            await expect(updatedExportedStories).rejects.toEqual(
                new BadRequestException(`История ${notPublishedStory?.id} не опубликована`)
            );
        });

        it('Возвращает ошибку, если не найдена история', async () => {
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

            const storyIdsToAdd = storyIds.slice(0, STORIES_EXPORT_LIMIT);

            await factory.createExportStories(
                storyIdsToAdd.length,
                storyIdsToAdd.map((storyId, index) => ({
                    storyId,
                    layer: LayerEnum.test,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            const positionToInsert = 2;

            const updatedExportedStories = exportStoriesService.addStoryToPosition({
                layer: LayerEnum.test,
                type: TypeEnum.default,
                position: positionToInsert,
                storyId: 100500,
            });

            await expect(updatedExportedStories).rejects.toEqual(new NotFoundException('История 100500 не найдена'));
        });

        it('Возвращает ошибку, если передана некорректная позиция', async () => {
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

            const storyIdsToAdd = storyIds.slice(0, STORIES_EXPORT_LIMIT);

            await factory.createExportStories(
                storyIdsToAdd.length,
                storyIdsToAdd.map((storyId, index) => ({
                    storyId,
                    layer: LayerEnum.test,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            const positionToInsert = STORIES_EXPORT_LIMIT;
            const storyToInsert = stories[5];
            const updatedExportedStories = exportStoriesService.addStoryToPosition({
                layer: LayerEnum.test,
                type: TypeEnum.default,
                position: positionToInsert,
                storyId: storyToInsert?.id || 0,
            });

            await expect(updatedExportedStories).rejects.toEqual(
                new BadRequestException(
                    `Некорректная позиция ${positionToInsert}. Допустимы позиции от 0 до ${STORIES_EXPORT_LIMIT - 1}`
                )
            );
        });
    });

    describe('remove', () => {
        it('Удаляет экпорт', async () => {
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

            const storyIdsToAdd = storyIds.slice(0, STORIES_EXPORT_LIMIT);

            const exportStories = await factory.createExportStories(
                storyIdsToAdd.length,
                storyIdsToAdd.map((storyId, index) => ({
                    storyId,
                    layer: LayerEnum.test,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            const deletedExportStory = exportStories[0];

            await exportStoriesService.remove({
                id: deletedExportStory?.id || 0,
                layer: LayerEnum.test,
                type: TypeEnum.default,
            });

            const exportStoriesAfterRemove = await exportStoriesService.findByLayerAndType({
                layer: LayerEnum.test,
                type: TypeEnum.default,
            });

            expect(exportStoriesAfterRemove.length).toBe(exportStories.length);
            expect(
                exportStoriesAfterRemove.find(exportStory => exportStory.id === deletedExportStory?.id)
            ).toBeUndefined();
            expect(
                exportStoriesAfterRemove.find(exportStory => exportStory.position === deletedExportStory?.position)
            ).toBeDefined();
        });

        it('Возвращает ошибку, если не найдено экспорта с переданным id', async () => {
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

            const storyIdsToAdd = storyIds.slice(0, STORIES_EXPORT_LIMIT);

            await factory.createExportStories(
                storyIdsToAdd.length,
                storyIdsToAdd.map((storyId, index) => ({
                    storyId,
                    layer: LayerEnum.test,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            const fakeId = 100500;

            const updatedExportedStories = exportStoriesService.remove({
                id: fakeId,
                layer: LayerEnum.test,
                type: TypeEnum.default,
            });

            await expect(updatedExportedStories).rejects.toEqual(new NotFoundException(`Экпорт ${fakeId} не найден`));
        });
    });

    describe('moveToPosition', () => {
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

            const storyIdsToAdd = storyIds.slice(0, STORIES_EXPORT_LIMIT);

            const exportStories = await factory.createExportStories(
                storyIdsToAdd.length,
                storyIdsToAdd.map((storyId, index) => ({
                    storyId,
                    layer: LayerEnum.test,
                    type: TypeEnum.default,
                    position: index,
                }))
            );

            const previousPositions = exportStories.map(({ position }) => position);

            const exportStoryToMove = exportStories[10];

            if (!exportStoryToMove) {
                throw 'Something bad happened';
            }
            const oldPosition = exportStoryToMove.position;
            const newPosition = oldPosition + 5;

            await exportStoriesService.moveToPosition({
                oldPosition,
                newPosition,
                layer: LayerEnum.test,
                type: TypeEnum.default,
            });

            const exportStoriesAfterMove = await exportStoriesService.findByLayerAndType({
                layer: LayerEnum.test,
                type: TypeEnum.default,
            });

            const newPositions = exportStoriesAfterMove.map(({ position }) => position);

            expect(newPositions).toEqual(previousPositions);

            const movedStory = exportStoriesAfterMove.find(({ id }) => id === exportStoryToMove.id);

            expect(movedStory?.position).toBe(newPosition);
        });
    });
});
