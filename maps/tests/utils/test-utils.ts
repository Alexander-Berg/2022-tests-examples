import {prepareDB} from './db-utils';
import {prepareServer} from './request-utils';
import {prepareNock} from './nock-utils';

export function prepareTests(): void {
    prepareDB();
    prepareServer();
    prepareNock();
}
