import { Test, TestingModule } from '@nestjs/testing';
import { describe } from '@jest/globals';

import { getFixtures } from '../../../tests/get-fixtures';
import { PollService } from '../poll.service';
import { PollModule } from '../poll.module';
import { Post as PostModel } from '../../post/post.model';
import { Poll } from '../poll.model';
import { Service } from '../../../types/common';

import { fixtures } from './poll.service.fixtures';

const DATE_NOW = '2021-09-08T12:30:35.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Poll service', () => {
    let testingModule: TestingModule;
    let pollService: PollService;

    beforeEach(async () => {
        testingModule = await Test.createTestingModule({
            imports: [PollModule],
        }).compile();

        pollService = await testingModule.resolve(PollService);
        Date.now = jest.fn().mockReturnValue(new Date(DATE_NOW));
    });

    afterEach(async () => {
        await testingModule.close();
    });
    describe('findByPk', () => {
        it('Возвращает null, если опрос не найден', async () => {
            const result = await pollService.findByPk(1);

            expect(result).toEqual(null);
        });
        it('Возвращает существующий опрос', async () => {
            const { POLL_DATA } = getFixtures(fixtures);

            const poll = await Poll.create(POLL_DATA);

            const result = await pollService.findByPk(poll.id);

            expect(result).toMatchObject(POLL_DATA);
        });
    });
    describe('update', () => {
        it('Обновляет опрос, если пост не опубликован', async () => {
            const { POST_ATTRIBUTES_1, POLL_DATA_1, POLL_UPDATE_DATA } = getFixtures(fixtures);

            const poll = await Poll.create(POLL_DATA_1);

            if (poll.answers) {
                await Promise.all(
                    poll.answers.map(async (answer, index) => {
                        await poll?.$create('statistic', {
                            answerIndex: index,
                            count: 5,
                        });
                    })
                );
            }

            await PostModel.create(POST_ATTRIBUTES_1);

            const result = await pollService.update(1, POLL_UPDATE_DATA);

            const statistics = await poll.$get('statistics');

            expect(statistics.every(({ count }) => count === 0)).toBeTruthy();
            expect(result).toMatchObject({
                id: 1,
                service: Service.autoru,
                ...POLL_UPDATE_DATA,
            });
        });

        it('Обновляет опрос, если пост опубликован и кол-во ответов не изменилось', async () => {
            const { POST_ATTRIBUTES_1, POLL_DATA_1, POLL_UPDATE_DATA } = getFixtures(fixtures);

            const poll = await Poll.create(POLL_DATA_1);

            if (poll.answers) {
                await Promise.all(
                    poll.answers.map(async (answer, index) => {
                        await poll?.$create('statistic', {
                            answerIndex: index,
                            count: 5,
                        });
                    })
                );
            }

            await PostModel.create(POST_ATTRIBUTES_1);

            const result = await pollService.update(1, POLL_UPDATE_DATA);

            const statistics = await poll.$get('statistics');

            expect(statistics.every(({ count }) => count === 5)).toBeTruthy();
            expect(result).toMatchObject({
                id: 1,
                service: Service.autoru,
                ...POLL_UPDATE_DATA,
            });
        });

        it('Возвращает ошибку, если обновить кол-во ответов и пост опубликован', async () => {
            const { POST_ATTRIBUTES_1, POLL_DATA_1, POLL_UPDATE_DATA } = getFixtures(fixtures);

            await Poll.create(POLL_DATA_1);

            await PostModel.create(POST_ATTRIBUTES_1);

            await expect(pollService.update(1, POLL_UPDATE_DATA)).rejects.toThrow('Опрос уже опубликован');
        });
    });
});
