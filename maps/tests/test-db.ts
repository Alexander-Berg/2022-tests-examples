import * as pg from 'pg';
import {executeReadCallback, executeInTransaction, executeModifyCallback} from 'src/lib/db';

/**
 * Sample data for some table.
 */
interface Fixture {
    /**
     * Table name.
     */
    table: string;
    rows: Record<string, any>[];
}

/**
 * Helper class for using database in tests.
 */
export class TestDb {
    private static readonly _skipTables = ['migrate_version'];
    private static readonly _schema = 'public';

    /**
     * Executes query.
     */
    async query(text: string, values?: any[]): Promise<pg.QueryResult> {
        return executeReadCallback(async (client) => {
            return client.query(text, values);
        });
    }

    /**
     * Loads sample data (fixtures) to database.
     * Prefer object version. Use array versions if you need multiple separate inserts to the same table.
     * Order of tables and keys in object matters, because of constraints in database.
     */
    loadFixtures(fixtures: Fixture[] | Record<string, Record<string, any>[]>): Promise<void> {
        return executeInTransaction(async (client) => {
            const entries = Array.isArray(fixtures) ? fixtures :
                Object.entries(fixtures).map(([table, rows]) => ({table, rows}));

            // Insert tables in the given order for satisfy constraints.
            for (const {table, rows} of entries) {
                const insertions = rows.map((row) => this._insertRowInTable(client, row, table));
                await Promise.all(insertions);
            }
        });
    }

    /**
     * Clean database.
     */
    async clean(): Promise<void> {
        await executeModifyCallback(async (client) => {
            const tableNamesResult = await client.query(
                `SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = '${TestDb._schema}'
                    AND table_type = 'BASE TABLE'`
            );

            if (!tableNamesResult.rowCount) {
                return;
            }

            const tableNames = tableNamesResult.rows
                .map((row) => row.table_name)
                .filter((tableName) => !TestDb._skipTables.includes(tableName))
                .map((tableName) => `${TestDb._schema}."${tableName}"`)
                .join(', ');

            if (!tableNames.length) {
                return;
            }

            await client.query(`TRUNCATE TABLE ${tableNames} RESTART IDENTITY`);
        });
    }

    private async _insertRowInTable(
        client: pg.PoolClient,
        row: Record<string, any>,
        table: string
    ): Promise<pg.QueryResult> {
        const columns = Object.keys(row);
        const values = columns.map((column) => row[column]);

        const queryStr = `INSERT INTO ${table} (${columns.join(', ')})
            VALUES (${generateQueryPlaceholders(values.length)})`;

        return client.query(queryStr, values);
    }
}

function generateQueryPlaceholders(count: number): string {
    const arr = [];
    for (let i = 1; i <= count; i++) {
        arr.push('$' + i);
    }
    return arr.join(', ');
}
