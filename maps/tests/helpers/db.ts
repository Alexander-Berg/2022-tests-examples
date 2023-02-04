import {dbClient} from 'app/lib/db-client';
import {decodePublicId} from 'app/v1/helpers/public-id';

export interface StoredSeed {
    seed: number;
}

export async function selectList(publicId: string): Promise<any> {
    const decodedPublicId = decodePublicId(publicId);
    const {data: {rows: savedLists}} = await dbClient.executeReadQuery({
        text: `SELECT * FROM lists
            WHERE id = $1 AND seed = $2`,
        values: [decodedPublicId!.id, decodedPublicId!.seed]
    });

    const parsedLists = savedLists
        .map((list: any) => ({
            ...list,
            id: Number(list.id),
            bookmarks: JSON.parse(list.bookmarks)
        }));

    return parsedLists[0];
}
