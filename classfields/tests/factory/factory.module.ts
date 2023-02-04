import { Module } from '@nestjs/common';
import { ExportStoriesModule } from 'stories-api/lib/modules/export-stories/export-stories.module';

import { StoryModule } from '../../modules/story/story.module';
import { StoryPageModule } from '../../modules/story-page/story-page.module';
import { TagModule } from '../../modules/tag/tag.module';

import { FactoryService } from './factory.service';

@Module({
    imports: [StoryModule, StoryPageModule, TagModule, ExportStoriesModule],
    exports: [FactoryService],
    providers: [FactoryService],
})
export class FactoryModule {}
