import { Test, TestingModule } from '@nestjs/testing';
import config from '@yandex-int/yandex-cfg';
import { NestExpressApplication } from '@nestjs/platform-express';
import { json, urlencoded } from 'body-parser';

import xRequestId from 'internal-core/server/middlewares/x-request-id';
import tracingMiddleware from 'internal-core/server/middlewares/tracing';
import tvmMiddleware from 'internal-core/server/middlewares/tvm';

import { FactoryService } from '../services/factory.service';
import { AppModule } from '../modules/app/app.module';

export async function createTestingApp() {
    const moduleFixture: TestingModule = await Test.createTestingModule({
        imports: [AppModule],
        providers: [FactoryService],
    }).compile();

    const factory = await moduleFixture.resolve(FactoryService);
    const app = await moduleFixture.createNestApplication<NestExpressApplication>();

    app.disable('x-powered-by');
    app.enable('trust proxy');
    app.set('etag', false);

    app.use(json({ limit: '50mb' }));
    app.use(urlencoded({ extended: true }));
    app.use(xRequestId());
    app.use(tracingMiddleware({ serviceName: config.serviceName, version: config.appVersion }));
    app.use(
        tvmMiddleware({
            destinations: [],
        })
    );
    app.set('json replacer', (key: string, value: unknown) => (value === null ? undefined : value));

    return {
        app,
        factory,
    };
}
