import {queryBuilder} from '../../app/lib/query-builder';
import * as db from '../../app/lib/db';
import {EntryWithTimestamps as Entry} from '../../app/typings';

export async function insertEntry(entry: Entry) {

    const query: string = queryBuilder
        .insert({
            project: entry.project,
            key: entry.key,
            data: JSON.stringify(entry.data),
            created_at: entry.created_at,
            updated_at: entry.updated_at,
            deleted_at: entry.deleted_at
        })
        .into('entries')
        .toString();

    await db.executeModifyQuery(query);
}

export async function clearEntries() {
    const query: string = queryBuilder('entries')
        .delete()
        .toString();

    await db.executeModifyQuery(query);
}
