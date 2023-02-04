import { Test, TestingModule } from '@nestjs/testing';
import { describe } from '@jest/globals';
import moment from 'moment';

import { getFixtures } from '../../../tests/get-fixtures';
import { Post as PostModel } from '../../post/post.model';
import { Tag as TagModel } from '../../tag/tag.model';
import { Category as CategoryModel } from '../../category/category.model';
import { SitemapService } from '../sitemap.service';
import { SitemapModule } from '../sitemap.module';

import { fixtures } from './sitemap.service.fixtures';

const DATE_NOW = '2021-09-08T12:30:35.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Sitemap service', () => {
    let testingModule: TestingModule;
    let sitemapService: SitemapService;

    beforeEach(async () => {
        testingModule = await Test.createTestingModule({
            imports: [SitemapModule],
        }).compile();

        sitemapService = await testingModule.resolve(SitemapService);
        Date.now = jest.fn().mockReturnValue(new Date(DATE_NOW));
    });

    afterEach(async () => {
        await testingModule.close();
    });

    describe('getSitemapElements', () => {
        it('Возвращает опубликованные открытые для индексирования посты', async () => {
            const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3, POST_ATTRIBUTES_4, POST_ATTRIBUTES_5 } =
                getFixtures(fixtures);

            const posts = await PostModel.bulkCreate([
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
            ]);

            const SITEMAP_POST_1 = posts[3] as PostModel;
            const SITEMAP_POST_2 = posts[4] as PostModel;

            const sitemapElements = await sitemapService.getSitemapElements();

            expect(sitemapElements).toEqual([
                {
                    pathname: `/journal/post/${SITEMAP_POST_1.urlPart}/`,
                    changefreq: 'daily',
                    priority: 1.0,
                    lastmod: moment(SITEMAP_POST_1.publishAt).format(moment.defaultFormatUtc),
                },
                {
                    pathname: `/journal/post/${SITEMAP_POST_2.urlPart}/`,
                    changefreq: 'daily',
                    priority: 1.0,
                    lastmod: moment(SITEMAP_POST_2.publishAt).format(moment.defaultFormatUtc),
                },
            ]);
        });

        it('Возвращает неархивные теги', async () => {
            const { TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2, TAG_ATTRIBUTES_3 } = getFixtures(fixtures);

            const tags = await TagModel.bulkCreate([TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2, TAG_ATTRIBUTES_3]);

            const SITEMAP_TAG_1 = tags[1] as TagModel;
            const SITEMAP_TAG_2 = tags[2] as TagModel;

            const sitemapElements = await sitemapService.getSitemapElements();

            expect(sitemapElements).toEqual([
                {
                    pathname: `/journal/tag/${SITEMAP_TAG_1.urlPart}/`,
                    changefreq: 'daily',
                    priority: 0.8,
                    lastmod: moment(DATE_NOW).format(moment.defaultFormatUtc),
                },
                {
                    pathname: `/journal/tag/${SITEMAP_TAG_2.urlPart}/`,
                    changefreq: 'daily',
                    priority: 0.8,
                    lastmod: moment(DATE_NOW).format(moment.defaultFormatUtc),
                },
            ]);
        });

        it('Возвращает неархивные категории', async () => {
            const { CATEGORY_ATTRIBUTES_1, CATEGORY_ATTRIBUTES_2, CATEGORY_ATTRIBUTES_3 } = getFixtures(fixtures);

            const categories = await CategoryModel.bulkCreate([
                CATEGORY_ATTRIBUTES_1,
                CATEGORY_ATTRIBUTES_2,
                CATEGORY_ATTRIBUTES_3,
            ]);

            const SITEMAP_CATEGORY_1 = categories[1] as CategoryModel;
            const SITEMAP_CATEGORY_2 = categories[2] as CategoryModel;

            const sitemapElements = await sitemapService.getSitemapElements();

            expect(sitemapElements).toEqual([
                {
                    pathname: `/journal/category/${SITEMAP_CATEGORY_1.urlPart}/`,
                    changefreq: 'daily',
                    priority: 0.8,
                    lastmod: moment(DATE_NOW).format(moment.defaultFormatUtc),
                },
                {
                    pathname: `/journal/category/${SITEMAP_CATEGORY_2.urlPart}/`,
                    changefreq: 'daily',
                    priority: 0.8,
                    lastmod: moment(DATE_NOW).format(moment.defaultFormatUtc),
                },
            ]);
        });
    });
});
