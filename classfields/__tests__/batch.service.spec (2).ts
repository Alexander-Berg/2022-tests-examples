import { Test } from '@nestjs/testing';

import { BatchPostRepository } from '../batch.post.repository';
import { PostSchemaMarkupService } from '../../../modules/post-schema-markup/post-schema-markup.service';
import { BatchModule } from '../batch.module';
import { BatchService } from '../batch.service';
import { getFixtures } from '../../../tests/get-fixtures';
import { Post as PostModel } from '../../../modules/post/post.model';
import { PostSchemaMarkupTypes } from '../../../types/post-schema-markup';
import { PostSchemaMarkup as PostSchemaMarkupModel } from '../../../modules/post-schema-markup/post-schema-markup.model';

import { fixtures } from './batch.service.fixtures';

describe('Autoru post schema markup generator batch service', () => {
    let batchService: BatchService;
    let batchPostRepository: BatchPostRepository;
    let postSchemaMarkupService: PostSchemaMarkupService;
    let spyDeleteNonActualFaqPageMarkups: jest.SpyInstance;
    let spyGetForNewFaqPageMarkup: jest.SpyInstance;
    let spyGetForUpdatableFaqPageMarkup: jest.SpyInstance;
    let spyCreatePostSchemaMarkup: jest.SpyInstance;
    let spyUpdatePostSchemaMarkup: jest.SpyInstance;
    let spyDeletePostSchemaMarkup: jest.SpyInstance;

    beforeEach(async () => {
        const module = await Test.createTestingModule({
            imports: [BatchModule],
        }).compile();

        batchService = await module.resolve(BatchService);
        batchPostRepository = await module.resolve(BatchPostRepository);
        postSchemaMarkupService = await module.resolve(PostSchemaMarkupService);

        spyDeleteNonActualFaqPageMarkups = jest.spyOn(postSchemaMarkupService, 'deleteNonActualFaqPageMarkups');
        spyGetForNewFaqPageMarkup = jest.spyOn(batchPostRepository, 'getForNewFaqPageMarkup');
        spyGetForUpdatableFaqPageMarkup = jest.spyOn(batchPostRepository, 'getForUpdatableFaqPageMarkup');
        spyCreatePostSchemaMarkup = jest.spyOn(postSchemaMarkupService, 'create');
        spyUpdatePostSchemaMarkup = jest.spyOn(postSchemaMarkupService, 'update');
        spyDeletePostSchemaMarkup = jest.spyOn(postSchemaMarkupService, 'delete');
    });

    describe('generateFaqPageMarkups', () => {
        it('Удаляет ненужную микроразметку постов', async () => {
            spyDeleteNonActualFaqPageMarkups.mockResolvedValue(undefined);
            spyGetForNewFaqPageMarkup.mockResolvedValue([]);
            spyGetForUpdatableFaqPageMarkup.mockResolvedValue([]);

            await expect(batchService.generateFaqPageMarkups()).resolves.toBeUndefined();
            expect(spyDeleteNonActualFaqPageMarkups).toHaveBeenCalledTimes(1);
            expect(spyDeleteNonActualFaqPageMarkups).toHaveBeenCalledWith({ userLogin: BatchService.batchName });
        });

        it('Не создает микроразметку для постов без блоков', async () => {
            const { POST_ATTRIBUTES } = getFixtures(fixtures);

            const POST_FOR_MARKUP = await PostModel.create(POST_ATTRIBUTES).then(model => model.reload());

            spyDeleteNonActualFaqPageMarkups.mockResolvedValue(undefined);
            spyGetForNewFaqPageMarkup.mockResolvedValue([POST_FOR_MARKUP]);
            spyGetForUpdatableFaqPageMarkup.mockResolvedValue([]);
            spyCreatePostSchemaMarkup.mockResolvedValue(undefined);

            await expect(batchService.generateFaqPageMarkups()).resolves.toBeUndefined();
            expect(spyDeleteNonActualFaqPageMarkups).toHaveBeenCalledTimes(1);
            expect(spyDeleteNonActualFaqPageMarkups).toHaveBeenCalledWith({ userLogin: BatchService.batchName });
            expect(spyCreatePostSchemaMarkup).not.toHaveBeenCalled();
            expect(spyUpdatePostSchemaMarkup).not.toHaveBeenCalled();
        });

        it('Создает микроразметку', async () => {
            const { POST_ATTRIBUTES } = getFixtures(fixtures);

            const POST_FOR_MARKUP = await PostModel.create(POST_ATTRIBUTES).then(model => model.reload());

            spyDeleteNonActualFaqPageMarkups.mockResolvedValue(undefined);
            spyGetForNewFaqPageMarkup.mockResolvedValue([POST_FOR_MARKUP]);
            spyGetForUpdatableFaqPageMarkup.mockResolvedValue([]);
            spyCreatePostSchemaMarkup.mockResolvedValue(undefined);

            await expect(batchService.generateFaqPageMarkups()).resolves.toBeUndefined();
            expect(spyDeleteNonActualFaqPageMarkups).toHaveBeenCalledTimes(1);
            expect(spyDeleteNonActualFaqPageMarkups).toHaveBeenCalledWith({ userLogin: BatchService.batchName });
            expect(spyCreatePostSchemaMarkup).toHaveBeenCalledTimes(1);
            expect(spyCreatePostSchemaMarkup).toHaveBeenCalledWith(
                {
                    postId: POST_FOR_MARKUP.id,
                    type: PostSchemaMarkupTypes.faqPage,
                    markup: {
                        mainEntity: [
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                        ],
                    },
                },
                { userLogin: BatchService.batchName }
            );
            expect(spyUpdatePostSchemaMarkup).not.toHaveBeenCalled();
        });

        it('Обновляет микроразметку', async () => {
            const { POST_ATTRIBUTES, POST_SCHEMA_MARKUP } = getFixtures(fixtures);

            const POST_FOR_MARKUP = await PostModel.create(POST_ATTRIBUTES).then(model => model.reload());
            const POST_MARKUP = await PostSchemaMarkupModel.create({
                postId: POST_FOR_MARKUP.id,
                ...POST_SCHEMA_MARKUP,
            }).then(model => model.reload());

            await POST_FOR_MARKUP.reload({
                include: [
                    {
                        model: PostSchemaMarkupModel,
                        as: 'faqPageSchemaMarkup',
                        required: false,
                    },
                ],
            });

            spyDeleteNonActualFaqPageMarkups.mockResolvedValue(undefined);
            spyGetForNewFaqPageMarkup.mockResolvedValue([]);
            spyGetForUpdatableFaqPageMarkup.mockResolvedValue([POST_FOR_MARKUP]);
            spyUpdatePostSchemaMarkup.mockResolvedValue(undefined);

            await expect(batchService.generateFaqPageMarkups()).resolves.toBeUndefined();
            expect(spyDeleteNonActualFaqPageMarkups).toHaveBeenCalledTimes(1);
            expect(spyDeleteNonActualFaqPageMarkups).toHaveBeenCalledWith({ userLogin: BatchService.batchName });
            expect(spyUpdatePostSchemaMarkup).toHaveBeenCalledTimes(1);
            expect(spyUpdatePostSchemaMarkup).toHaveBeenCalledWith(
                POST_MARKUP.id,
                {
                    markup: {
                        mainEntity: [
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                            {
                                acceptedAnswer: {
                                    text: '<p>Почти никаких!</p>',
                                },
                                name: 'В чём особенности зарядки электромобиля?',
                            },
                        ],
                    },
                },
                { userLogin: BatchService.batchName }
            );
        });

        it('Удаляет микроразметку, если она стала пустой', async () => {
            const { POST_ATTRIBUTES, POST_SCHEMA_MARKUP } = getFixtures(fixtures);

            const POST_FOR_MARKUP = await PostModel.create(POST_ATTRIBUTES).then(model => model.reload());
            const POST_MARKUP = await PostSchemaMarkupModel.create({
                postId: POST_FOR_MARKUP.id,
                ...POST_SCHEMA_MARKUP,
            }).then(model => model.reload());

            await POST_FOR_MARKUP.reload({
                include: [
                    {
                        model: PostSchemaMarkupModel,
                        as: 'faqPageSchemaMarkup',
                        required: false,
                    },
                ],
            });

            spyDeleteNonActualFaqPageMarkups.mockResolvedValue(undefined);
            spyGetForNewFaqPageMarkup.mockResolvedValue([]);
            spyGetForUpdatableFaqPageMarkup.mockResolvedValue([POST_FOR_MARKUP]);
            spyUpdatePostSchemaMarkup.mockResolvedValue(undefined);

            await expect(batchService.generateFaqPageMarkups()).resolves.toBeUndefined();
            expect(spyDeleteNonActualFaqPageMarkups).toHaveBeenCalledWith({ userLogin: BatchService.batchName });
            expect(spyUpdatePostSchemaMarkup).not.toHaveBeenCalled();
            expect(spyDeletePostSchemaMarkup).toHaveBeenCalledWith(POST_MARKUP.id, {
                userLogin: BatchService.batchName,
            });
        });
    });
});
