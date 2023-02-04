import {dbClient} from 'app/lib/db-client';

export async function clearDb() {
    await dbClient.executeWriteQuery({
        text: `SELECT truncate_tables()`
    });
}
