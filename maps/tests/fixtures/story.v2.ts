import faker from 'faker';
import {
    RequestV2,
    StoryScreen,
    StoryScreenButtonLinkType,
    StoryScreenOpenUrlButton,
    StoryScreenPhoto
} from '../../app/v2/types';

export const BIZ_IDS = ['123456', '654321', '111999', '333555'];

export function getTestStory(...bizIds: string[]): RequestV2.CreateStory {
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

export function getTestStoryScreen(): StoryScreen {
    return {
        photo: getTestStoryContentItem(),
        buttons: [getOpenUrlButton()]
    };
}

export function getTestStoryContentItem(): StoryScreenPhoto {
    return {
        urlTemplate: faker.image.imageUrl() + '/%s'
    };
}

export function getOpenUrlButton(): StoryScreenOpenUrlButton {
    return {
        openUrlButton: {
            linkType: StoryScreenButtonLinkType.EXTERNAL_LINK,
            title: faker.commerce.productName().slice(0, 14),
            url: faker.internet.url()
        }
    };
}
