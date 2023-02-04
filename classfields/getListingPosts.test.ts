import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import journalApi from 'auto-core/server/resources/journal-api/getResource.nock.fixtures';
import type { TDescriptContext } from 'auto-core/server/descript/createContext';
import getArticlesFixtures from 'auto-core/server/resources/journal-api/methods/getArticles.fixtures';

import getListingPosts from './getListingPosts';

const context = { req: createHttpReq(), res: createHttpRes() } as unknown as TDescriptContext;

it('возвращает getPosts ресурс, если одна категория и один тег', () => {
    const params = {
        categories: [ 'news' ],
        tags: [ 'bmw' ],
    };

    journalApi
        .get(`/posts`)
        .query({
            status: 'publish',
            orderBySort: 'publishAt',
            pageNumber: '0',
            pageSize: '10',
            tags: params.tags[0],
            categories: params.categories[0],
            offset: '0',
            forFeed: false,
        })
        .reply(200, getArticlesFixtures.response200());

    return de.run(getListingPosts, { context, params }).then(
        (result: any) => {
            expect(result.posts).toHaveLength(3);
            expect(result.pagination).toMatchSnapshot();
        },
    );
});

describe('возвращает getPostsForSection ресурс, если', () => {
    it('две категории', () => {
        const params = {
            categories: [ 'news', 'pro' ],
        };

        journalApi
            .get(`/postsForSection`)
            .query({
                service: 'autoru',
                status: 'publish',
                orderBySort: 'publishAt',
                pageNumber: '0',
                pageSize: '10',
                categories: params.categories,
                offset: '0',
            })
            .reply(200, getArticlesFixtures.response200());

        return de.run(getListingPosts, { context, params }).then(
            (result: any) => {
                expect(result.posts).toHaveLength(3);
                expect(result.pagination).toMatchSnapshot();
            },
        );
    });

    it('два тега', () => {
        const params = {
            tags: [ 'bmw', 'sdelano-v-kitae' ],
        };

        journalApi
            .get(`/postsForSection`)
            .query({
                service: 'autoru',
                status: 'publish',
                orderBySort: 'publishAt',
                pageNumber: '0',
                pageSize: '10',
                tags: params.tags,
                offset: '0',
            })
            .reply(200, getArticlesFixtures.response200());

        return de.run(getListingPosts, { context, params }).then(
            (result: any) => {
                expect(result.posts).toHaveLength(3);
                expect(result.pagination).toMatchSnapshot();
            },
        );
    });
});
