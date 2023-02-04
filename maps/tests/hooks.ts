import nock from 'nock';
import db, {knex} from 'app/lib/db-client';

const mochaHooks = {
    async beforeEach(): Promise<void> {
        await db.executeWriteQuery(async ({execute}) => {
            await execute(knex.raw('TRUNCATE TABLE bookings CASCADE'));
            await execute(knex.raw('TRUNCATE TABLE feedback CASCADE'));
            await execute(knex.raw('TRUNCATE TABLE partners CASCADE'));
            await execute(knex.raw('TRUNCATE TABLE services_cache CASCADE'));
            await execute(knex.raw('TRUNCATE TABLE dynamic_config CASCADE'));
        });

        await db.executeWriteQuery(({execute}) =>
            execute(
                knex('partners').insert({
                    id: 'yclients',
                    name: 'YClients',
                    active: true,
                    endpoint: 'http://partner'
                })
            )
        );
    },

    afterEach(): void {
        nock.cleanAll();
    }
};
export {mochaHooks};
