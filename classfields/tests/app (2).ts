import { Test, TestingModule } from '@nestjs/testing';
import { NestExpressApplication } from '@nestjs/platform-express';
import { json, urlencoded } from 'body-parser';
import { getConnection } from 'typeorm';

import { AppModule } from '../modules/app/app.module';

import { FactoryModule } from './factory/factory.module';
import { FactoryService } from './factory/factory.service';

export interface ITestingApplication {
    factory: FactoryService;
    module: TestingModule;
    initNestApp: () => Promise<{ server: unknown }>;
    close: () => Promise<void>;
}

export async function createTestingApp(): Promise<ITestingApplication> {
    const testingModule: TestingModule = await Test.createTestingModule({
        imports: [AppModule, FactoryModule],
    }).compile();

    let app: NestExpressApplication;

    const factory = await testingModule.resolve(FactoryService);

    const initNestApp = async () => {
        app = await testingModule.createNestApplication<NestExpressApplication>();

        app.disable('x-powered-by');
        app.enable('trust proxy');
        app.set('etag', false);

        app.use(json({ limit: '50mb' }));
        app.use(urlencoded({ extended: true }));
        app.set('json replacer', (key: string, value: unknown) => (value === null ? undefined : value));

        await app.init();

        return {
            server: app.getHttpServer(),
        };
    };

    const closeApp = async () => {
        await getConnection().close();
        await app?.close();
    };

    return {
        factory,
        module: testingModule,
        initNestApp,
        close: closeApp,
    };
}
