import {promises as fs} from 'fs';
import * as path from 'path';

import {dbClient} from '../../app/lib/db-client';
import {BusinessDbRecord, StoryDbRecord} from '../../app/types/db';

async function cleanDB(): Promise<void> {
    const truncateTables = await fs.readFile(path.resolve(__dirname, '../../../resources/db/clean-test-tables.sql'), {
        encoding: 'utf8'
    });

    await dbClient.executeWriteQuery(truncateTables);
}

export function prepareDB(): void {
    beforeEach(async () => {
        await cleanDB();
    });
}

export async function runWriteQuery(query: string): Promise<void> {
    await dbClient.executeWriteCallback(async (client) => {
        await client.query(query);
    });
}

export async function getAllStories(): Promise<StoryDbRecord[]> {
    const result = await dbClient.executeReadQuery('SELECT * from stories');
    return (result.data.rows as StoryDbRecord[]).sort((rowA, rowB) => rowA.id.localeCompare(rowB.id));
}

type BusinessStoryEntry = {
    id: string;
    order: number;
};

export type AllBusinesses = Record<string, BusinessStoryEntry[]>;

export async function getAllBusinesses(): Promise<AllBusinesses> {
    const result = await dbClient.executeReadQuery('SELECT * from business_stories');
    return (result.data.rows as BusinessDbRecord[]).reduce<Record<string, BusinessStoryEntry[]>>((memo, row) => {
        const id = row.biz_id;
        const item = {
            id: row.story_id,
            order: row.story_order
        };
        if (!memo[id]) {
            memo[id] = [item];
        } else {
            memo[id].push(item);
            memo[id].sort((a, b) => a.order - b.order);
        }
        return memo;
    }, {});
}
