import * as Long from 'long';
import {StoriesIntTypes} from '../../app/proto-schemas/stories-int/types';
import faker from 'faker';

export const BIZ_IDS = ['123456', '654321', '111999', '333555'].map((id) => Long.fromString(id, true));

export function getTestStory(...bizIds: Long[]): StoriesIntTypes.StoryCreateRequest {
    return {
        bizIds,
        data: {
            title: faker.commerce.productName().slice(0, 14),
            coverUrlTemplate: faker.image.imageUrl() + '/%s',
            screens: [getTestStoryScreen()],
            tags: []
        }
    };
}

export function getTestStoryScreen(): StoriesIntTypes.StoryScreen {
    return {
        photo: getTestStoryContentItem(),
        buttons: [getOpenUrlButton()]
    };
}

export function getTestStoryContentItem(): StoriesIntTypes.StoryScreenPhoto {
    return {
        urlTemplate: faker.image.imageUrl() + '/%s'
    };
}

export function getOpenUrlButton(): StoriesIntTypes.StoryScreenOpenUrlButton {
    return {
        openUrlButton: {
            linkType: StoriesIntTypes.StoryScreenButtonLinkType.EXTERNAL_LINK,
            title: faker.commerce.productName().slice(0, 14),
            url: faker.internet.url()
        }
    };
}
