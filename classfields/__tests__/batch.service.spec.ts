import { Buffer } from 'buffer';

import { Test } from '@nestjs/testing';
import { expect } from '@jest/globals';

import { Post as PostModel } from '../../../modules/post/post.model';
import { PostService } from '../../../modules/post/post.service';
import { ActionsLogModule } from '../../../modules/actions-log/actions-log.module';
import { BatchService } from '../batch.service';
import { BatchModule } from '../batch.module';
import { getFixtures } from '../../../tests/get-fixtures';

import { fixtures } from './batch.service.fixtures';

const DATE_NOW = '2021-09-08T12:30:35.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

const mockGetBadLinksFile = jest.fn();
const mockRemoveBadLinksFile = jest.fn();

jest.mock('internal-core/server/resources/s3-mag-auto', () => ({
    getBadLinksFile: () => mockGetBadLinksFile(),
    removeBadLinksFile: () => mockRemoveBadLinksFile(),
}));

const mockLogInfo = jest.fn();
const mockLogError = jest.fn();
const mockLogWarn = jest.fn();

jest.mock('internal-core/server/logger', () => ({
    info: info => mockLogInfo(info),
    warn: warn => mockLogWarn(warn),
    error: error => mockLogError(error),
}));

describe('Autoru broken links fixer batch', () => {
    let postService: PostService;
    let spyPostServiceUpdate: jest.SpyInstance;
    let batchService: BatchService;

    beforeEach(async () => {
        const module = await Test.createTestingModule({
            imports: [BatchModule, ActionsLogModule],
        }).compile();

        postService = await module.resolve(PostService);
        spyPostServiceUpdate = jest.spyOn(postService, 'update');

        batchService = await module.resolve(BatchService);
    });

    it('Выводит предупреждение, если ресурс возвращает ошибку', async () => {
        const ERROR = 'Unknown s3 error occurred!';

        mockGetBadLinksFile.mockRejectedValue(ERROR);

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockLogWarn).toHaveBeenCalledWith(`Couldn't fetch csv file with bad links. Error: ${ERROR}`);
        expect(mockLogWarn).toHaveBeenCalledTimes(1);
    });

    it('Выводит предупреждение, если csv файл пуст', async () => {
        mockGetBadLinksFile.mockResolvedValue(Buffer.from(''));

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockLogWarn).toHaveBeenCalledWith('File is empty');
        expect(mockLogWarn).toHaveBeenCalledTimes(1);
    });

    it('Не выбрасывает ошибку, если в csv файле нет битых ссылок для замены', async () => {
        mockGetBadLinksFile.mockResolvedValue(Buffer.from('postUrlPart;badLink;newLink\n'));

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockLogError).toHaveBeenCalledTimes(0);
    });

    it('Выводит ошибку, если csv содержит пустые postUrlPart и badLink', async () => {
        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from('a;b;newLink\n' + 'uaz-patriot;http://bad-link.com;http://good-link.com')
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(mockLogError).toHaveBeenCalledWith(
            'Fixing bad link failed: ' + 'postUrlPart ("undefined"), badLink ("undefined") cannot be empty!'
        );
        expect(mockLogError).toHaveBeenCalledTimes(1);
    });

    it('Выводит ошибку, если csv не содержит newLink', async () => {
        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from('postUrlPart;badLink;c\n' + 'uaz-patriot;http://bad-link.com;http://good-link.com')
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(mockLogError).toHaveBeenCalledWith('Fixing bad link failed: newLink must be present in csv file!');
        expect(mockLogError).toHaveBeenCalledTimes(1);
    });

    it('Удаляет csv файл после обработки', async () => {
        mockGetBadLinksFile.mockResolvedValue(Buffer.from('postUrlPart;badLink;newLink\n'));

        await batchService.fix();

        expect(mockRemoveBadLinksFile).toHaveBeenCalledTimes(1);
        expect(mockLogError).toHaveBeenCalledTimes(0);
    });

    it('Может заменять битые ссылки на пустоту', async () => {
        const BAD_LINK = 'https://some-bad-link.com';
        const NEW_LINK = '';
        const BAD_LEAD = `<a href="${BAD_LINK}">Рассекретили серийную версию городского микроэлектромобиля Microlino 2.0</a>`;
        const NEW_LEAD = '<a href="">Рассекретили серийную версию городского микроэлектромобиля Microlino 2.0</a>';

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            lead: BAD_LEAD,
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from('postUrlPart;badLink;newLink\n' + `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n`)
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            lead: NEW_LEAD,
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Выводит ошибку, если пост не найден', async () => {
        const NON_EXISTING_URL_PART = 'non-existing-url-part';
        const BAD_LINK = 'http://bad-link.com';
        const NEW_LINK = 'http://new-link.com';

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from('postUrlPart;badLink;newLink\n' + `${NON_EXISTING_URL_PART};${BAD_LINK};${NEW_LINK}`)
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(mockLogError).toHaveBeenCalledWith(
            `Fixing bad link failed: post with urlPart "${NON_EXISTING_URL_PART}" was not found!`
        );
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(0);
    });

    it('Заменяет битые ссылки в лиде поста', async () => {
        const BAD_LINK_1 = 'https://some-bad-link.com';
        const BAD_LINK_2 = 'http://another-bad-link.ru/dangerous-path/';
        const NEW_LINK_1 = 'https://zen.yandex.com/posts/microlino-2.0/';
        const NEW_LINK_2 = 'http://mag.auto.ru/articles/microlino';
        const BAD_LEAD = `Рассекретили (${BAD_LINK_1}) серийную версию городского микроэлектромобиля Microlino 2.0 (https://link.com). Читать - ${BAD_LINK_2}`;
        const NEW_LEAD = `Рассекретили (${NEW_LINK_1}) серийную версию городского микроэлектромобиля Microlino 2.0 (https://link.com). Читать - ${NEW_LINK_2}`;

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            lead: BAD_LEAD,
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from(
                'postUrlPart;badLink;newLink\n' +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_1};${NEW_LINK_1}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_2};${NEW_LINK_2}\n`
            )
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            lead: NEW_LEAD,
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Не обновляет пост, если в нем нет битых ссылок', async () => {
        const LEAD = 'Почитайте про серийную версию городского микроэлектромобиля Microlino 2.0: https://mag.auto.ru';
        const TEXT_BLOCK = {
            type: 'text',
            text: 'Рассекретили серийную версию городского микроэлектромобиля Microlino 2.0: https://mag.auto.ru',
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            lead: LEAD,
            blocks: [TEXT_BLOCK],
            draftBlocks: [TEXT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from(
                'postUrlPart;badLink;newLink\n' +
                    `${POST_ATTRIBUTES.urlPart};https://some-bad-link.com;https://zen.yandex.com/posts/microlino-2.0/\n`
            )
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(0);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" skipped.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "text"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const TEXT_BLOCK = {
            type: 'text',
            text: `Рассекретили <a href="${BAD_LINK}">серийную</a> версию городского микроэлектромобиля Microlino 2.0`,
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [TEXT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from('postUrlPart;badLink;newLink\n' + `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n`)
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'text',
                    text: `Рассекретили <a href="${NEW_LINK}">серийную</a> версию городского микроэлектромобиля Microlino 2.0`,
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "imageWithDescription"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const BAD_LINK_2 = 'https://very-bad-link.com/';
        const NEW_LINK_2 = 'https://mag.auto.ru/article/microlino-2.0/';
        const BLOCK = {
            type: 'imageWithDescription',
            imageWithDescription: {
                image: null,
                imageTitle: `Рассекретили (${BAD_LINK}) серийную версию городского микроэлектромобиля Microlino 2.0`,
                description: `Рассекретили серийную версию городского микроэлектромобиля Microlino 2.0. <a href="${BAD_LINK_2}">Читать далее</a>`,
            },
        };
        const DRAFT_BLOCK = {
            type: 'imageWithDescription',
            imageWithDescription: {
                image: null,
                imageTitle: `${BAD_LINK} - Китайскому электрокару Xpeng P7 (${BAD_LINK_2}) добавили серийную версию с «Ламбо-дверьми»`,
                description: `Китайскому электрокару Xpeng P7 <a href="${BAD_LINK_2}">добавили</a> серийную версию с «Ламбо-дверьми». <a href="${BAD_LINK}">Читать</a>`,
            },
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from(
                'postUrlPart;badLink;newLink\n' +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_2};${NEW_LINK_2}\n`
            )
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'imageWithDescription',
                    imageWithDescription: {
                        image: null,
                        imageTitle: `Рассекретили (${NEW_LINK}) серийную версию городского микроэлектромобиля Microlino 2.0`,
                        description: `Рассекретили серийную версию городского микроэлектромобиля Microlino 2.0. <a href="${NEW_LINK_2}">Читать далее</a>`,
                    },
                },
            ],
            draftBlocks: [
                {
                    type: 'imageWithDescription',
                    imageWithDescription: {
                        image: null,
                        imageTitle: `${NEW_LINK} - Китайскому электрокару Xpeng P7 (${NEW_LINK_2}) добавили серийную версию с «Ламбо-дверьми»`,
                        description: `Китайскому электрокару Xpeng P7 <a href="${NEW_LINK_2}">добавили</a> серийную версию с «Ламбо-дверьми». <a href="${NEW_LINK}">Читать</a>`,
                    },
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "cardWithImages"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const BAD_LINK_2 = 'https://very-bad-link.com/';
        const NEW_LINK_2 = 'https://mag.auto.ru/article/microlino-2.0/';
        const BLOCK = {
            type: 'cardWithImages',
            cardWithImages: [
                {
                    type: 'imageWithDescription',
                    imageWithDescription: {
                        image: null,
                        imageTitle: `Рассекретили (${BAD_LINK}) серийную версию городского микроэлектромобиля Microlino 2.0`,
                        description: `Рассекретили серийную версию городского микроэлектромобиля Microlino 2.0. <a href="${BAD_LINK_2}">Читать далее</a>`,
                    },
                },
                {
                    type: 'code',
                    code: `<a href="${BAD_LINK}">link1</a><a href="${BAD_LINK_2}">link2</a>`,
                },
                {
                    type: 'text',
                    text: `10 очень стильных тюнинг-проектов на базе <a href="${BAD_LINK}">Toyota Land Cruiser</a>`,
                },
            ],
        };
        const DRAFT_BLOCK = {
            type: 'cardWithImages',
            cardWithImages: [
                {
                    type: 'imageWithDescription',
                    imageWithDescription: {
                        image: null,
                        imageTitle: `${BAD_LINK} - Китайскому электрокару Xpeng P7 (${BAD_LINK_2}) добавили серийную версию с «Ламбо-дверьми»`,
                        description: `Китайскому электрокару Xpeng P7 <a href="${BAD_LINK_2}">добавили</a> серийную версию с «Ламбо-дверьми». <a href="${BAD_LINK}">Читать</a>`,
                    },
                },
                {
                    type: 'code',
                    code: `<a href="${BAD_LINK}">link1</a><a href="${BAD_LINK_2}">link2</a>`,
                },
                {
                    type: 'text',
                    text: `10 очень стильных тюнинг-проектов на базе <a href="${BAD_LINK}">Toyota Land Cruiser</a>`,
                },
            ],
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from(
                'postUrlPart;badLink;newLink\n' +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_2};${NEW_LINK_2}\n`
            )
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'cardWithImages',
                    cardWithImages: [
                        {
                            type: 'imageWithDescription',
                            imageWithDescription: {
                                image: null,
                                imageTitle: `Рассекретили (${NEW_LINK}) серийную версию городского микроэлектромобиля Microlino 2.0`,
                                description: `Рассекретили серийную версию городского микроэлектромобиля Microlino 2.0. <a href="${NEW_LINK_2}">Читать далее</a>`,
                            },
                        },
                        {
                            type: 'code',
                            code: `<a href="${NEW_LINK}">link1</a><a href="${NEW_LINK_2}">link2</a>`,
                        },
                        {
                            type: 'text',
                            text: `10 очень стильных тюнинг-проектов на базе <a href="${NEW_LINK}">Toyota Land Cruiser</a>`,
                        },
                    ],
                },
            ],
            draftBlocks: [
                {
                    type: 'cardWithImages',
                    cardWithImages: [
                        {
                            type: 'imageWithDescription',
                            imageWithDescription: {
                                image: null,
                                imageTitle: `${NEW_LINK} - Китайскому электрокару Xpeng P7 (${NEW_LINK_2}) добавили серийную версию с «Ламбо-дверьми»`,
                                // eslint-disable-next-line max-len
                                description: `Китайскому электрокару Xpeng P7 <a href="${NEW_LINK_2}">добавили</a> серийную версию с «Ламбо-дверьми». <a href="${NEW_LINK}">Читать</a>`,
                            },
                        },
                        {
                            type: 'code',
                            code: `<a href="${NEW_LINK}">link1</a><a href="${NEW_LINK_2}">link2</a>`,
                        },
                        {
                            type: 'text',
                            text: `10 очень стильных тюнинг-проектов на базе <a href="${NEW_LINK}">Toyota Land Cruiser</a>`,
                        },
                    ],
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "autoPriceStat"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/some-bad-path.php';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const BAD_LINK_2 = 'https://very-bad-link.com/';
        const NEW_LINK_2 = 'https://mag.auto.ru/article/microlino-2.0/';
        const BLOCK = {
            type: 'autoPriceStat',
            autoPriceStat: {
                listUrl: `${BAD_LINK} `,
                statUrl: `  ${BAD_LINK_2} `,
            },
        };
        const DRAFT_BLOCK = {
            type: 'autoPriceStat',
            autoPriceStat: {
                listUrl: BAD_LINK,
                statUrl: BAD_LINK_2,
            },
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from(
                'postUrlPart;badLink;newLink\n' +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_2};${NEW_LINK_2}\n`
            )
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'autoPriceStat',
                    autoPriceStat: {
                        listUrl: `${NEW_LINK} `,
                        statUrl: `  ${NEW_LINK_2} `,
                    },
                },
            ],
            draftBlocks: [
                {
                    type: 'autoPriceStat',
                    autoPriceStat: {
                        listUrl: NEW_LINK,
                        statUrl: NEW_LINK_2,
                    },
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "card"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/some-bad-path.php';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const BAD_LINK_2 = 'https://very-bad-link.com/';
        const NEW_LINK_2 = 'https://mag.auto.ru/article/microlino-2.0/';
        const BLOCK = {
            type: 'card',
            card: {
                url: BAD_LINK,
                title: `Читайте- ${BAD_LINK} и тут ${BAD_LINK_2}`,
                text: `Подробнее читайте в этих статьях: <a href="${BAD_LINK}">раз</a>, <a href="${BAD_LINK_2}">два</a>`,
            },
        };
        const DRAFT_BLOCK = {
            type: 'card',
            card: {
                url: BAD_LINK,
                title: `Читайте- ${BAD_LINK} и тут ${BAD_LINK_2}`,
                text: `Подробнее можно почитать в этих статьях: <a href="${BAD_LINK}">раз</a>, <a href="${BAD_LINK_2}">два</a>`,
            },
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from(
                'postUrlPart;badLink;newLink\n' +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_2};${NEW_LINK_2}\n`
            )
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'card',
                    card: {
                        url: NEW_LINK,
                        title: `Читайте- ${NEW_LINK} и тут ${NEW_LINK_2}`,
                        text: `Подробнее читайте в этих статьях: <a href="${NEW_LINK}">раз</a>, <a href="${NEW_LINK_2}">два</a>`,
                    },
                },
            ],
            draftBlocks: [
                {
                    type: 'card',
                    card: {
                        url: NEW_LINK,
                        title: `Читайте- ${NEW_LINK} и тут ${NEW_LINK_2}`,
                        text: `Подробнее можно почитать в этих статьях: <a href="${NEW_LINK}">раз</a>, <a href="${NEW_LINK_2}">два</a>`,
                    },
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "htmlCard"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/some-bad-path.php';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const BAD_LINK_2 = 'https://very-bad-link.com/';
        const NEW_LINK_2 = 'https://mag.auto.ru/article/microlino-2.0/';
        const BLOCK = {
            type: 'htmlCard',
            htmlCard: `<a href="${BAD_LINK}" target="_blank">Статья 1</a>
                       <a href="${BAD_LINK_2}" target="_blank">Статья 2</a>`,
        };
        const DRAFT_BLOCK = {
            type: 'htmlCard',
            htmlCard: `<a href="${BAD_LINK}" target="_blank">Экономим на регистрации автомобиля в ГИБДД</a>
                       <a href="${BAD_LINK_2}" target="_blank">Материал по теме</a>`,
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from(
                'postUrlPart;badLink;newLink\n' +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_2};${NEW_LINK_2}\n`
            )
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'htmlCard',
                    htmlCard: `<a href="${NEW_LINK}" target="_blank">Статья 1</a>
                       <a href="${NEW_LINK_2}" target="_blank">Статья 2</a>`,
                },
            ],
            draftBlocks: [
                {
                    type: 'htmlCard',
                    htmlCard: `<a href="${NEW_LINK}" target="_blank">Экономим на регистрации автомобиля в ГИБДД</a>
                       <a href="${NEW_LINK_2}" target="_blank">Материал по теме</a>`,
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "code"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/some-bad-path.php';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const BAD_LINK_2 = 'https://very-bad-link.com/';
        const NEW_LINK_2 = 'https://mag.auto.ru/article/microlino-2.0/';
        const BLOCK = {
            type: 'code',
            code: `<a href="${BAD_LINK}">link1</a><a href="${BAD_LINK_2}">link2</a>`,
        };
        const DRAFT_BLOCK = {
            type: 'code',
            code: `<a href="${BAD_LINK_2}">link1</a><a href="${BAD_LINK}">link2</a>`,
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from(
                'postUrlPart;badLink;newLink\n' +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_2};${NEW_LINK_2}\n`
            )
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'code',
                    code: `<a href="${NEW_LINK}">link1</a><a href="${NEW_LINK_2}">link2</a>`,
                },
            ],
            draftBlocks: [
                {
                    type: 'code',
                    code: `<a href="${NEW_LINK_2}">link1</a><a href="${NEW_LINK}">link2</a>`,
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "gallery"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/some-bad-path.php';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const BAD_LINK_2 = 'https://very-bad-link.com/';
        const NEW_LINK_2 = 'https://mag.auto.ru/article/microlino-2.0/';
        const BAD_LINK_3 = 'https://mail.ru/news';
        const NEW_LINK_3 = 'https://ya.ru/';
        const BLOCK = {
            type: 'gallery',
            gallery: [
                {
                    title: `Смотрите: ${BAD_LINK}`,
                    description: `<a href="${BAD_LINK_2}">Смотрите</a>`,
                    sourceUrl: BAD_LINK_3,
                },
            ],
        };
        const DRAFT_BLOCK = {
            type: 'gallery',
            gallery: [
                {
                    title: `Смотрите: ${BAD_LINK}`,
                    description: `<a href="${BAD_LINK_2}">Смотрите</a>`,
                    sourceUrl: BAD_LINK_3,
                },
                {
                    title: `Читайте также: ${BAD_LINK}`,
                    description: `<a href="${BAD_LINK_2}">Читайте также</a>`,
                    sourceUrl: BAD_LINK_3,
                },
            ],
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from(
                'postUrlPart;badLink;newLink\n' +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_2};${NEW_LINK_2}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_3};${NEW_LINK_3}\n`
            )
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'gallery',
                    gallery: [
                        {
                            title: `Смотрите: ${NEW_LINK}`,
                            description: `<a href="${NEW_LINK_2}">Смотрите</a>`,
                            sourceUrl: NEW_LINK_3,
                        },
                    ],
                },
            ],
            draftBlocks: [
                {
                    type: 'gallery',
                    gallery: [
                        {
                            title: `Смотрите: ${NEW_LINK}`,
                            description: `<a href="${NEW_LINK_2}">Смотрите</a>`,
                            sourceUrl: NEW_LINK_3,
                        },
                        {
                            title: `Читайте также: ${NEW_LINK}`,
                            description: `<a href="${NEW_LINK_2}">Читайте также</a>`,
                            sourceUrl: NEW_LINK_3,
                        },
                    ],
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "highlightedText"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const BAD_LINK_2 = 'https://very-bad-link.com/';
        const NEW_LINK_2 = 'https://mag.auto.ru/article/microlino-2.0/';
        const BLOCK = {
            type: 'highlightedText',
            highlightedText: `Рассекретили (${BAD_LINK}) серийную версию городского микроэлектромобиля Microlino 2.0`,
        };
        const DRAFT_BLOCK = {
            type: 'highlightedText',
            highlightedText: `${BAD_LINK} - рассекретили серийную версию городского микроэлектромобиля Microlino 2.0. ${BAD_LINK_2}`,
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from(
                'postUrlPart;badLink;newLink\n' +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_2};${NEW_LINK_2}\n`
            )
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'highlightedText',
                    highlightedText: `Рассекретили (${NEW_LINK}) серийную версию городского микроэлектромобиля Microlino 2.0`,
                },
            ],
            draftBlocks: [
                {
                    type: 'highlightedText',
                    highlightedText: `${NEW_LINK} - рассекретили серийную версию городского микроэлектромобиля Microlino 2.0. ${NEW_LINK_2}`,
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "offersAutoru"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const BAD_LINK_2 = 'https://very-bad-link.com/';
        const NEW_LINK_2 = 'https://mag.auto.ru/article/microlino-2.0/';
        const BLOCK = {
            type: 'offersAutoru',
            offersAutoru: {
                urls: [BAD_LINK],
            },
        };
        const DRAFT_BLOCK = {
            type: 'offersAutoru',
            offersAutoru: {
                urls: [BAD_LINK, BAD_LINK_2],
            },
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from(
                'postUrlPart;badLink;newLink\n' +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_2};${NEW_LINK_2}\n`
            )
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'offersAutoru',
                    offersAutoru: {
                        urls: [NEW_LINK],
                    },
                },
            ],
            draftBlocks: [
                {
                    type: 'offersAutoru',
                    offersAutoru: {
                        urls: [NEW_LINK, NEW_LINK_2],
                    },
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "post"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const BAD_LINK_2 = 'https://very-bad-link.com/';
        const NEW_LINK_2 = 'https://mag.auto.ru/article/microlino-2.0/';
        const BLOCK = {
            type: 'post',
            post: {
                title: `Читать еще: ${BAD_LINK}`,
            },
        };
        const DRAFT_BLOCK = {
            type: 'post',
            post: {
                title: `Еще статьи: ${BAD_LINK}, ${BAD_LINK_2}`,
            },
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from(
                'postUrlPart;badLink;newLink\n' +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_2};${NEW_LINK_2}\n`
            )
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'post',
                    post: {
                        title: `Читать еще: ${NEW_LINK}`,
                    },
                },
            ],
            draftBlocks: [
                {
                    type: 'post',
                    post: {
                        title: `Еще статьи: ${NEW_LINK}, ${NEW_LINK_2}`,
                    },
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "quiz"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const BAD_LINK_2 = 'https://very-bad-link.com/';
        const NEW_LINK_2 = 'https://mag.auto.ru/article/microlino-2.0/';
        const BAD_LINK_3 = 'https://mail.ru/news';
        const NEW_LINK_3 = 'https://ya.ru/';
        const BLOCK = {
            type: 'quiz',
            quiz: {
                results: [
                    {
                        title: `Вы победили! Вам сюда - ${BAD_LINK}`,
                        imageTitle: `Победа - ${BAD_LINK}`,
                        description: `Поздравляем, вы ответили правильно на все вопросы, вам <a href="${BAD_LINK}">сюда</a>`,
                    },
                ],
                questions: [
                    {
                        question: `Вопрос <a href="${BAD_LINK_3}">1</a>?`,
                        imageTitle: `Вопрос 1 - ${BAD_LINK_3}`,
                        answers: [
                            {
                                title: `Ответ на вопрос 1 ${BAD_LINK_3}`,
                                imageTitle: `Ответ 1 ${BAD_LINK_3}`,
                            },
                        ],
                        rightAnswer: {
                            title: `Правильный ответ 1 ${BAD_LINK_3}`,
                            description: `Это правильный <a href="${BAD_LINK_3}">ответ</a> 1`,
                        },
                        incorrectAnswer: {
                            title: `Неправильный ответ 1 ${BAD_LINK_3}`,
                            description: `Это неправильный <a href="${BAD_LINK_3}">ответ</a> 1`,
                        },
                    },
                ],
            },
        };
        const DRAFT_BLOCK = {
            type: 'quiz',
            quiz: {
                results: [
                    {
                        title: `Вы победили! Вам сюда - ${BAD_LINK}`,
                        imageTitle: `Победа - ${BAD_LINK}`,
                        description: `Поздравляем, вы ответили правильно на все вопросы, вам <a href="${BAD_LINK}">сюда</a>`,
                    },
                    {
                        title: `Вы проиграли! Вам сюда - ${BAD_LINK_2}`,
                        imageTitle: `Победа - ${BAD_LINK_2}`,
                        description: `Поздравляем, вы ответили правильно на все вопросы, вам <a href="${BAD_LINK_2}">сюда</a>`,
                    },
                ],
                questions: [
                    {
                        question: `Вопрос <a href="${BAD_LINK_3}">1</a>?`,
                        imageTitle: `Вопрос 1 - ${BAD_LINK_3}`,
                        answers: [
                            {
                                title: `Ответ на вопрос 1 ${BAD_LINK_3}`,
                                imageTitle: `Ответ 1 ${BAD_LINK_3}`,
                            },
                        ],
                        rightAnswer: {
                            title: `Правильный ответ 1 ${BAD_LINK_3}`,
                            description: `Это правильный <a href="${BAD_LINK_3}">ответ</a> 1`,
                        },
                        incorrectAnswer: {
                            title: `Неправильный ответ 1 ${BAD_LINK_3}`,
                            description: `Это неправильный <a href="${BAD_LINK_3}">ответ</a> 1`,
                        },
                    },
                    {
                        question: `Вопрос <a href="${BAD_LINK_3}">2</a>?`,
                        imageTitle: `Вопрос 2 - ${BAD_LINK_3}`,
                        answers: [
                            {
                                title: `Ответ на вопрос 2 ${BAD_LINK_3}`,
                                imageTitle: `Ответ 2 ${BAD_LINK_3}`,
                            },
                        ],
                        rightAnswer: {
                            title: `Правильный ответ 2 ${BAD_LINK_3}`,
                            description: `Это правильный <a href="${BAD_LINK_3}">ответ</a> 2`,
                        },
                        incorrectAnswer: {
                            title: `Неправильный ответ 2 ${BAD_LINK_3}`,
                            description: `Это неправильный <a href="${BAD_LINK_3}">ответ</a> 2`,
                        },
                    },
                ],
            },
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from(
                'postUrlPart;badLink;newLink\n' +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_2};${NEW_LINK_2}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_3};${NEW_LINK_3}\n`
            )
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'quiz',
                    quiz: {
                        results: [
                            {
                                title: `Вы победили! Вам сюда - ${NEW_LINK}`,
                                imageTitle: `Победа - ${NEW_LINK}`,
                                description: `Поздравляем, вы ответили правильно на все вопросы, вам <a href="${NEW_LINK}">сюда</a>`,
                            },
                        ],
                        questions: [
                            {
                                question: `Вопрос <a href="${NEW_LINK_3}">1</a>?`,
                                imageTitle: `Вопрос 1 - ${NEW_LINK_3}`,
                                answers: [
                                    {
                                        title: `Ответ на вопрос 1 ${NEW_LINK_3}`,
                                        imageTitle: `Ответ 1 ${NEW_LINK_3}`,
                                    },
                                ],
                                rightAnswer: {
                                    title: `Правильный ответ 1 ${NEW_LINK_3}`,
                                    description: `Это правильный <a href="${NEW_LINK_3}">ответ</a> 1`,
                                },
                                incorrectAnswer: {
                                    title: `Неправильный ответ 1 ${NEW_LINK_3}`,
                                    description: `Это неправильный <a href="${NEW_LINK_3}">ответ</a> 1`,
                                },
                            },
                        ],
                    },
                },
            ],
            draftBlocks: [
                {
                    type: 'quiz',
                    quiz: {
                        results: [
                            {
                                title: `Вы победили! Вам сюда - ${NEW_LINK}`,
                                imageTitle: `Победа - ${NEW_LINK}`,
                                description: `Поздравляем, вы ответили правильно на все вопросы, вам <a href="${NEW_LINK}">сюда</a>`,
                            },
                            {
                                title: `Вы проиграли! Вам сюда - ${NEW_LINK_2}`,
                                imageTitle: `Победа - ${NEW_LINK_2}`,
                                description: `Поздравляем, вы ответили правильно на все вопросы, вам <a href="${NEW_LINK_2}">сюда</a>`,
                            },
                        ],
                        questions: [
                            {
                                question: `Вопрос <a href="${NEW_LINK_3}">1</a>?`,
                                imageTitle: `Вопрос 1 - ${NEW_LINK_3}`,
                                answers: [
                                    {
                                        title: `Ответ на вопрос 1 ${NEW_LINK_3}`,
                                        imageTitle: `Ответ 1 ${NEW_LINK_3}`,
                                    },
                                ],
                                rightAnswer: {
                                    title: `Правильный ответ 1 ${NEW_LINK_3}`,
                                    description: `Это правильный <a href="${NEW_LINK_3}">ответ</a> 1`,
                                },
                                incorrectAnswer: {
                                    title: `Неправильный ответ 1 ${NEW_LINK_3}`,
                                    description: `Это неправильный <a href="${NEW_LINK_3}">ответ</a> 1`,
                                },
                            },
                            {
                                question: `Вопрос <a href="${NEW_LINK_3}">2</a>?`,
                                imageTitle: `Вопрос 2 - ${NEW_LINK_3}`,
                                answers: [
                                    {
                                        title: `Ответ на вопрос 2 ${NEW_LINK_3}`,
                                        imageTitle: `Ответ 2 ${NEW_LINK_3}`,
                                    },
                                ],
                                rightAnswer: {
                                    title: `Правильный ответ 2 ${NEW_LINK_3}`,
                                    description: `Это правильный <a href="${NEW_LINK_3}">ответ</a> 2`,
                                },
                                incorrectAnswer: {
                                    title: `Неправильный ответ 2 ${NEW_LINK_3}`,
                                    description: `Это неправильный <a href="${NEW_LINK_3}">ответ</a> 2`,
                                },
                            },
                        ],
                    },
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "quota"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const BAD_LINK_2 = 'https://very-bad-link.com/';
        const NEW_LINK_2 = 'https://mag.auto.ru/article/microlino-2.0/';
        const BAD_LINK_3 = 'https://mail.ru/news';
        const NEW_LINK_3 = 'https://ya.ru/';
        const BLOCK = {
            type: 'quota',
            quota: {
                url: BAD_LINK,
                title: `Источник цитаты: ${BAD_LINK}`,
                text: `<a href="${BAD_LINK_2}">Умное</a>`,
                author: ` ${BAD_LINK_3}`,
                imageTitle: `Персона - ${BAD_LINK_3}`,
            },
        };
        const DRAFT_BLOCK = {
            type: 'quota',
            quota: {
                url: BAD_LINK,
                title: `Источник цитаты - ${BAD_LINK}`,
                text: `<a href="${BAD_LINK_2}">Умная цитата</a>`,
                author: `${BAD_LINK_3}`,
                imageTitle: `Автор цитаты - ${BAD_LINK_3}`,
            },
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from(
                'postUrlPart;badLink;newLink\n' +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_2};${NEW_LINK_2}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_3};${NEW_LINK_3}\n`
            )
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'quota',
                    quota: {
                        url: NEW_LINK,
                        title: `Источник цитаты: ${NEW_LINK}`,
                        text: `<a href="${NEW_LINK_2}">Умное</a>`,
                        author: ` ${NEW_LINK_3}`,
                        imageTitle: `Персона - ${NEW_LINK_3}`,
                    },
                },
            ],
            draftBlocks: [
                {
                    type: 'quota',
                    quota: {
                        url: NEW_LINK,
                        title: `Источник цитаты - ${NEW_LINK}`,
                        text: `<a href="${NEW_LINK_2}">Умная цитата</a>`,
                        author: `${NEW_LINK_3}`,
                        imageTitle: `Автор цитаты - ${NEW_LINK_3}`,
                    },
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "table"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const BAD_LINK_2 = 'https://very-bad-link.com/';
        const NEW_LINK_2 = 'https://mag.auto.ru/article/microlino-2.0/';
        const BAD_LINK_3 = 'https://mail.ru/news';
        const NEW_LINK_3 = 'https://ya.ru/';
        const BLOCK = {
            type: 'table',
            table: {
                title: `Источник данных: ${BAD_LINK}`,
                data: [[BAD_LINK_2], [BAD_LINK_3]],
            },
        };
        const DRAFT_BLOCK = {
            type: 'table',
            table: {
                title: `Источник данных - ${BAD_LINK}`,
                data: [[BAD_LINK], [BAD_LINK_2], [BAD_LINK_3], ['foo']],
            },
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from(
                'postUrlPart;badLink;newLink\n' +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_2};${NEW_LINK_2}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_3};${NEW_LINK_3}\n`
            )
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'table',
                    table: {
                        title: `Источник данных: ${NEW_LINK}`,
                        data: [[NEW_LINK_2], [NEW_LINK_3]],
                    },
                },
            ],
            draftBlocks: [
                {
                    type: 'table',
                    table: {
                        title: `Источник данных - ${NEW_LINK}`,
                        data: [[NEW_LINK], [NEW_LINK_2], [NEW_LINK_3], ['foo']],
                    },
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "tth"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const BLOCK = {
            type: 'tth',
            tth: {
                url: `Ссылка на комплектацию: ${BAD_LINK}`,
            },
        };
        const DRAFT_BLOCK = {
            type: 'tth',
            tth: {
                url: `Авто.ру: ${BAD_LINK}`,
            },
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from('postUrlPart;badLink;newLink\n' + `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n`)
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'tth',
                    tth: {
                        url: `Ссылка на комплектацию: ${NEW_LINK}`,
                    },
                },
            ],
            draftBlocks: [
                {
                    type: 'tth',
                    tth: {
                        url: `Авто.ру: ${NEW_LINK}`,
                    },
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "video"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';

        const BLOCK = {
            type: 'video',
            video: {
                videoUrl: BAD_LINK,
                description: `Интересное <a href="${BAD_LINK}">видео</a> по теме`,
            },
        };
        const DRAFT_BLOCK = {
            type: 'video',
            video: {
                videoUrl: BAD_LINK,
                description: `Еще одно интересное <a href="${BAD_LINK}">видео</a> по теме`,
            },
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from('postUrlPart;badLink;newLink\n' + `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n`)
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'video',
                    video: {
                        videoUrl: NEW_LINK,
                        description: `Интересное <a href="${NEW_LINK}">видео</a> по теме`,
                    },
                },
            ],
            draftBlocks: [
                {
                    type: 'video',
                    video: {
                        videoUrl: NEW_LINK,
                        description: `Еще одно интересное <a href="${NEW_LINK}">видео</a> по теме`,
                    },
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "youtube"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';

        const BLOCK = {
            type: 'youtube',
            youtube: {
                videoUrl: BAD_LINK,
                title: `Интересное видео по теме: ${BAD_LINK}`,
            },
        };
        const DRAFT_BLOCK = {
            type: 'youtube',
            youtube: {
                videoUrl: BAD_LINK,
                title: `Еще одно интересное видео по теме: ${BAD_LINK}`,
            },
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from('postUrlPart;badLink;newLink\n' + `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n`)
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'youtube',
                    youtube: {
                        videoUrl: NEW_LINK,
                        title: `Интересное видео по теме: ${NEW_LINK}`,
                    },
                },
            ],
            draftBlocks: [
                {
                    type: 'youtube',
                    youtube: {
                        videoUrl: NEW_LINK,
                        title: `Еще одно интересное видео по теме: ${NEW_LINK}`,
                    },
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "bubble"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const BAD_LINK_2 = 'https://very-bad-link.com/';
        const NEW_LINK_2 = 'https://mag.auto.ru/article/microlino-2.0/';

        const BLOCK = {
            type: 'bubble',
            bubble: {
                title: `Читать: ${BAD_LINK}, ${BAD_LINK_2}`,
                content: `Еще интересные материалы: <a href="${BAD_LINK}">раз</a>, <a href="${BAD_LINK_2}">два</a>`,
            },
        };
        const DRAFT_BLOCK = {
            type: 'bubble',
            bubble: {
                title: `Читать: ${BAD_LINK}, ${BAD_LINK_2}`,
                content: `Еще больше интересных материалов: <a href="${BAD_LINK}">раз</a>, <a href="${BAD_LINK_2}">два</a>`,
            },
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from(
                'postUrlPart;badLink;newLink\n' +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_2};${NEW_LINK_2}\n`
            )
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'bubble',
                    bubble: {
                        title: `Читать: ${NEW_LINK}, ${NEW_LINK_2}`,
                        content: `Еще интересные материалы: <a href="${NEW_LINK}">раз</a>, <a href="${NEW_LINK_2}">два</a>`,
                    },
                },
            ],
            draftBlocks: [
                {
                    type: 'bubble',
                    bubble: {
                        title: `Читать: ${NEW_LINK}, ${NEW_LINK_2}`,
                        content: `Еще больше интересных материалов: <a href="${NEW_LINK}">раз</a>, <a href="${NEW_LINK_2}">два</a>`,
                    },
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Заменяет битые ссылки в блоке "person"', async () => {
        const BAD_LINK = 'https://some-bad-link.com/';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const BAD_LINK_2 = 'https://very-bad-link.com/';
        const NEW_LINK_2 = 'https://mag.auto.ru/article/microlino-2.0/';

        const BLOCK = {
            type: 'person',
            person: {
                title: `Цитата: ${BAD_LINK}, ${BAD_LINK_2}`,
                subtitle: `Больше материалов от эксперта: ${BAD_LINK}, ${BAD_LINK_2}`,
            },
        };
        const DRAFT_BLOCK = {
            type: 'person',
            person: {
                title: `Мнение эксперта: ${BAD_LINK}, ${BAD_LINK_2}`,
                subtitle: `Еще больше материалов от эксперта: ${BAD_LINK}, ${BAD_LINK_2}`,
            },
        };

        const { POST_ATTRIBUTES } = getFixtures(fixtures);

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [BLOCK],
            draftBlocks: [DRAFT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from(
                'postUrlPart;badLink;newLink\n' +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n` +
                    `${POST_ATTRIBUTES.urlPart};${BAD_LINK_2};${NEW_LINK_2}\n`
            )
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                {
                    type: 'person',
                    person: {
                        title: `Цитата: ${NEW_LINK}, ${NEW_LINK_2}`,
                        subtitle: `Больше материалов от эксперта: ${NEW_LINK}, ${NEW_LINK_2}`,
                    },
                },
            ],
            draftBlocks: [
                {
                    type: 'person',
                    person: {
                        title: `Мнение эксперта: ${NEW_LINK}, ${NEW_LINK_2}`,
                        subtitle: `Еще больше материалов от эксперта: ${NEW_LINK}, ${NEW_LINK_2}`,
                    },
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });

    it('Не меняет блоки, в которых нет ссылок', async () => {
        const { POST_ATTRIBUTES, IMAGE } = getFixtures(fixtures);

        const BAD_LINK = 'https://some-bad-link.com/';
        const NEW_LINK = 'https://zen.yandex.com/posts/microlino-2.0/';
        const POLL_BLOCK = { type: 'poll', poll: { id: 10 } };
        const TEXT_BLOCK = {
            type: 'text',
            text: `10 очень стильных <a href="${BAD_LINK}">тюнинг-проектов</a> на базе Toyota Land Cruiser`,
        };
        const IMAGE_BLOCK = { type: 'image', image: { image: IMAGE, height: '100px' } };

        await PostModel.create({
            ...POST_ATTRIBUTES,
            blocks: [POLL_BLOCK, TEXT_BLOCK, IMAGE_BLOCK],
            draftBlocks: [IMAGE_BLOCK, POLL_BLOCK, TEXT_BLOCK],
        });

        mockGetBadLinksFile.mockResolvedValue(
            Buffer.from('postUrlPart;badLink;newLink\n' + `${POST_ATTRIBUTES.urlPart};${BAD_LINK};${NEW_LINK}\n`)
        );

        const result = await batchService.fix();

        expect(result).toBeUndefined();
        expect(mockGetBadLinksFile).toHaveBeenCalledTimes(1);
        expect(spyPostServiceUpdate).toHaveBeenCalledWith(POST_ATTRIBUTES.urlPart, {
            userLogin: batchService.batchName,
            blocks: [
                POLL_BLOCK,
                {
                    type: 'text',
                    text: `10 очень стильных <a href="${NEW_LINK}">тюнинг-проектов</a> на базе Toyota Land Cruiser`,
                },
                IMAGE_BLOCK,
            ],
            draftBlocks: [
                IMAGE_BLOCK,
                POLL_BLOCK,
                {
                    type: 'text',
                    text: `10 очень стильных <a href="${NEW_LINK}">тюнинг-проектов</a> на базе Toyota Land Cruiser`,
                },
            ],
        });
        expect(spyPostServiceUpdate).toHaveBeenCalledTimes(1);
        expect(mockLogInfo).toHaveBeenCalledWith(`Fixing post with urlPart "${POST_ATTRIBUTES.urlPart}" succeeded.`);
        expect(mockLogInfo).toHaveBeenCalledTimes(1);
    });
});
