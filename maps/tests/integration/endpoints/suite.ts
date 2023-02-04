import {TestDb} from 'tests/test-db';
import {TestServer} from 'tests/integration/test-server';

export interface EndpointSuiteContext {
    db: TestDb;
    server: TestServer;
}

export type EndpointSuiteDefinition = (
    this: Mocha.ISuiteCallbackContext,
    context: EndpointSuiteContext
) => void;
