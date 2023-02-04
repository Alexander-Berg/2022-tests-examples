import * as fs from 'fs';
import {randomBytes} from 'crypto';
import {encodePublicId} from 'app/v1/helpers/public-id';
import {random, pseudoRandom} from 'tests/helpers/generation';

const generateUid = pseudoRandom(42);

const AMMOS_FOLDER = 'resources/stress/ammos';

main();

async function main() {
    if (!fs.existsSync(AMMOS_FOLDER)) {
        fs.mkdirSync(AMMOS_FOLDER, {recursive: true});
    }

    // Generate ammos
    // tslint:disable-next-line: no-console
    console.log('Generating ammos...');

    const ammosCount = 10000;

    generateSyncSharedListsAmmo(ammosCount, `${AMMOS_FOLDER}/sync_shared_lists`);
    generateGetListsAmmo(ammosCount, 'my_shared', `${AMMOS_FOLDER}/get_lists_my_shared`);
    generateGetListsAmmo(ammosCount, 'my_subscriptions', `${AMMOS_FOLDER}/get_lists_my_subscriptions`);
    generateGetListsAmmo(ammosCount, 'array', `${AMMOS_FOLDER}/get_lists_array`);
    generateChangeSubscriptionAmmo(ammosCount, `${AMMOS_FOLDER}/change_subscription`);
    generateGetSubscriptionsStatusesAmmo(ammosCount, `${AMMOS_FOLDER}/get_subscriptions_statuses`);
}

function generateSyncSharedListsAmmo(ammosCount: number, ammoFilePath: string) {
    if (fs.existsSync(ammoFilePath)) {
        fs.unlinkSync(ammoFilePath);
    }

    const endpoint = '/v1/sync_shared_lists';

    const writeStream = fs.createWriteStream(ammoFilePath);

    for (let i = 0; i < ammosCount; i++) {
        const body = generateRandomSyncSharedListsBody();
        const data = Buffer.from(JSON.stringify(body));
        const randomUid = generateUid();

        const header = Buffer.from([
            `POST ${endpoint} HTTP/1.1`,
            `Host: bookmarks-int-stress.tst.c.maps.yandex.net`,
            `Content-Length: ${data.byteLength}`,
            `Content-Type: application/json`,
            `Connection: keep-alive`,
            `uid: ${randomUid}`,
            '\r\n'
        ].join('\r\n'));

        const shell = Buffer.concat([
            Buffer.from(`${header.byteLength + data.byteLength}\n`),
            header,
            data
        ]);

        writeStream.write(shell);
        writeStream.write('\n\n');
    }
}

function generateGetListsAmmo(ammosCount: number, mode: string, ammoFilePath: string) {
    if (fs.existsSync(ammoFilePath)) {
        fs.unlinkSync(ammoFilePath);
    }

    const endpoint = '/v1/get_lists';

    const writeStream = fs.createWriteStream(ammoFilePath);

    // Export id and seed stored in the database and save them in the lists_ids.json file
    const publicIds = JSON.parse(fs.readFileSync('resources/stress/db-data/lists_ids.json', 'utf8'))
        .map((item: any) => encodePublicId(item.id, Number(item.seed)));

    // Export the uids and save them in the uids.json file
    const uids = JSON.parse(fs.readFileSync('resources/stress/db-data/uids.json', 'utf8'))
        .map((item: any) => item.uid);

    for (let i = 0; i < ammosCount; i++) {
        const body: any = {};
        switch (mode) {
            case 'my_shared': {
                body.only = 'my_shared';
                break;
            }
            case 'my_subscriptions': {
                body.only = 'my_subscriptions';
                break;
            }
            case 'array': {
                const listsCount = random(1, 15);
                body.only = [];
                for (let i = 0; i < listsCount; i++) {
                    const randomPublicIdIndex = random(0, publicIds.length - 1);
                    body.only.push(publicIds[randomPublicIdIndex]);
                }
                break;
            }
        }

        const data = Buffer.from(JSON.stringify(body));
        const uid = uids[random(0, uids.length - 1)];

        const header = Buffer.from([
            `POST ${endpoint} HTTP/1.1`,
            `Host: bookmarks-int-stress.tst.c.maps.yandex.net`,
            `Content-Length: ${data.byteLength}`,
            `Content-Type: application/json`,
            `Connection: keep-alive`,
            `uid: ${uid}`,
            '\r\n'
        ].join('\r\n'));

        const shell = Buffer.concat([
            Buffer.from(`${header.byteLength + data.byteLength}\n`),
            header,
            data
        ]);

        writeStream.write(shell);
        writeStream.write('\n\n');
    }
}

function generateChangeSubscriptionAmmo(ammosCount: number, ammoFilePath: string) {
    if (fs.existsSync(ammoFilePath)) {
        fs.unlinkSync(ammoFilePath);
    }

    const endpoint = '/v1/change_subscription';

    const writeStream = fs.createWriteStream(ammoFilePath);

    // Export id and seed stored in the database and save them in the lists_ids.json file
    const publicIds = JSON.parse(fs.readFileSync('resources/stress/db-data/lists_ids.json', 'utf8'))
        .map((item: any) => encodePublicId(item.id, Number(item.seed)));

    for (let i = 0; i < ammosCount / 2; i++) {
        const randomUid = random(1000, 2000);
        const randomPublicId = publicIds[random(0, publicIds.length - 1)];

        for (const mode of ['add', 'remove']) {
            const data = Buffer.from(JSON.stringify({[mode]: randomPublicId}));

            const header = Buffer.from([
                `POST ${endpoint} HTTP/1.1`,
                `Host: bookmarks-int-stress.tst.c.maps.yandex.net`,
                `Content-Length: ${data.byteLength}`,
                `Content-Type: application/json`,
                `Connection: keep-alive`,
                `uid: ${randomUid}`,
                '\r\n'
            ].join('\r\n'));

            const shell = Buffer.concat([
                Buffer.from(`${header.byteLength + data.byteLength}\n`),
                header,
                data
            ]);

            writeStream.write(shell);
            writeStream.write('\n\n');
        }
    }
}

function generateGetSubscriptionsStatusesAmmo(ammosCount: number, ammoFilePath: string) {
    if (fs.existsSync(ammoFilePath)) {
        fs.unlinkSync(ammoFilePath);
    }

    const endpoint = '/v1/get_subscriptions_statuses';

    const writeStream = fs.createWriteStream(ammoFilePath);

    // Export the uids and save them in the uids.json file
    const uids = JSON.parse(fs.readFileSync('resources/stress/db-data/uids.json', 'utf8'))
        .map((item: any) => item.uid);

    for (let i = 0; i < ammosCount; i++) {
        const query = random(0, 1) ? '?add_fields=title' : '';
        const uid = uids[random(0, uids.length - 1)];

        const header = Buffer.from([
            `GET ${endpoint}${query} HTTP/1.1`,
            `Host: bookmarks-int-stress.tst.c.maps.yandex.net`,
            `Connection: keep-alive`,
            `uid: ${uid}`,
            '\r\n'
        ].join('\r\n'));

        const shell = Buffer.concat([
            Buffer.from(`${header.byteLength}\n`),
            header
        ]);

        writeStream.write(shell);
    }
}

function generateRandomSyncSharedListsBody() {
    const bodyLength = random(10, 20);
    const body = [];
    const statuses = ['shared', 'closed', 'deleted'];
    for (let i = 0; i < bodyLength; i++) {
        const bookmarksCount = random(5, 15);
        const bookmarks = [];

        for (let j = 0; j < bookmarksCount; j++) {
            bookmarks.push({
                record_id: randomBytes(18).toString('hex'),
                title: randomBytes(10).toString('hex'),
                uri: 'https://' + randomBytes(10).toString('hex'),
                description: randomBytes(15).toString('hex')
            });
        }

        body.push({
            record_id: randomBytes(18).toString('hex'),
            revision: random(1, 100),
            status: statuses[random(0, 2)],
            title: randomBytes(10).toString('hex'),
            bookmarks
        });
    }
    return body;
}
