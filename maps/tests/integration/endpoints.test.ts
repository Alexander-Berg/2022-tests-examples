import * as glob from 'glob';
import {createApp} from 'src/app';
import {TestDb} from 'tests/test-db';
import {TestServer} from 'tests/integration/test-server';
import {TvmDaemon} from 'tests/integration/tvm-daemon';
import {EndpointSuiteDefinition, EndpointSuiteContext} from './endpoints/suite';

describe('endpoints', function () {
    // Increase timeout for working with database.
    this.timeout(process.env.CI ? 60_000 : 10_000);

    const db = new TestDb();
    let server: TestServer;
    let tvmDaemon: TvmDaemon;

    // Use type assertion, because we must change object by reference in `before` hook.
    const context = {} as EndpointSuiteContext;

    before(async () => {
        // Clean database before first test, because we don't know its state.
        await db.clean();

        const app = await createApp();

        server = await TestServer.start(app);
        tvmDaemon = await TvmDaemon.start();

        context.db = db;
        context.server = server;
    });

    after(async () => {
        await Promise.all([
            tvmDaemon.stop(),
            server.stop()
        ]);
    });

    afterEach(async () => {
        await db.clean();
    });

    const filePaths = glob.sync('./endpoints/**/*.test.js', {cwd: __dirname});
    filePaths.forEach((filePath) => {
        const main: EndpointSuiteDefinition = require(filePath).main;
        main.call(this, context);
    });
});
