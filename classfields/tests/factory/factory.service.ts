import { Injectable } from '@nestjs/common';

import { StoryModel } from '../../modules/story/story.model';
import { StoryFactory } from '../../modules/story/story.factory';
import { StoryPageModel } from '../../modules/story-page/story-page.model';
import { StoryPageFactory } from '../../modules/story-page/story-page.factory';
import { TagFactory } from '../../modules/tag/tag.factory';
import { TagModel } from '../../modules/tag/tag.model';
import { ExportStoriesFactory } from '../../modules/export-stories/export-stories.factory';
import { ExportStoriesModel } from '../..//modules/export-stories/export-stories.model';

@Injectable()
export class FactoryService {
    constructor(
        private storyFactory: StoryFactory,
        private storyPageFactory: StoryPageFactory,
        private tagFactory: TagFactory,
        private exportStoriesFactory: ExportStoriesFactory
    ) {}

    public createStory(attributes?: Partial<StoryModel>): Promise<StoryModel> {
        return this.storyFactory.create(attributes);
    }

    public createStoryWithTags(attributes?: Partial<StoryModel> & { tags: string[] }): Promise<StoryModel> {
        return this.storyFactory.createWithTags(attributes);
    }

    public createStories(count = 1, attributes?: Array<Partial<StoryModel>>): Promise<StoryModel[]> {
        return this.storyFactory.createMany(count, attributes);
    }

    public createStoriesWithTags(
        count = 1,
        attributes: Array<Partial<StoryModel> & { tags: string[] }> = []
    ): Promise<StoryModel[]> {
        return this.storyFactory.createManyWithTags(count, attributes);
    }

    public createStoryPage(attributes?: Partial<StoryPageModel>): Promise<StoryPageModel> {
        return this.storyPageFactory.create(attributes);
    }

    public createStoryPages(count = 1, attributes?: Array<Partial<StoryPageModel>>): Promise<StoryPageModel[]> {
        return this.storyPageFactory.createMany(count, attributes);
    }

    public createTag(attributes?: Partial<TagModel>): Promise<TagModel> {
        return this.tagFactory.create(attributes);
    }

    public createTags(count = 1, attributes?: Array<Partial<TagModel>>): Promise<TagModel[]> {
        return this.tagFactory.createMany(count, attributes);
    }

    public createExportStories(
        count = 1,
        attributes?: Array<Partial<ExportStoriesModel>>
    ): Promise<ExportStoriesModel[]> {
        return this.exportStoriesFactory.createMany(count, attributes);
    }
}
