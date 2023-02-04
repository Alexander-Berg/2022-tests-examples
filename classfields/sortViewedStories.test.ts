import type { TStoryCover } from 'auto-core/react/dataDomain/stories/TStories';

import mock from '../mock';

import sortViewedStories from './sortViewedStories';

let stories: Array<TStoryCover>;

beforeEach(() => {
    stories = mock.stories.value().map(({ data }) => data);
});

it('должен выбрать все просмотренные сториз, убрать в конец списка и проставить им флаг', () => {
    const params = {
        stories,
        viewedStoryIds: [ stories[1].id, stories[3].id ].join(),
    };
    const result = sortViewedStories(params);

    expect(result?.map(({ id }) => id)).toEqual([
        stories[0].id,
        stories[2].id,
        stories[4].id,
        stories[1].id,
        stories[3].id,
    ]);
    expect(result?.[3].is_viewed).toEqual(true);
    expect(result?.[4].is_viewed).toEqual(true);
});

it('должен просто отдать исходный массив, если куки нет', () => {
    const params = {
        stories,
    };
    const result = sortViewedStories(params);

    expect(result?.map(({ id }) => id)).toEqual([
        stories[0].id,
        stories[1].id,
        stories[2].id,
        stories[3].id,
        stories[4].id,
    ]);
    expect(result?.some((story) => story.is_viewed)).toEqual(false);
});

it('должен просто отдать исходный массив, если все id в куке устарели', () => {
    const params = {
        stories,
        viewedStoryIds: [ 'bla-bla', 'bla-bla-bla', 'bla-bla-bla-bla' ].join(),
    };
    const result = sortViewedStories(params);

    expect(result?.map(({ id }) => id)).toEqual([
        stories[0].id,
        stories[1].id,
        stories[2].id,
        stories[3].id,
        stories[4].id,
    ]);
    expect(result?.some((story) => story.is_viewed)).toEqual(false);
});

it('не упадет, если сториз нет', () => {
    const params = {
        viewedStoryIds: [ stories[1].id, stories[3].id ].join(),
    };

    expect(sortViewedStories(params)).toEqual([]);
});
