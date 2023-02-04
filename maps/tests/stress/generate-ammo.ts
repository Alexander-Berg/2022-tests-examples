/**
 * Generates ammo for stress testing.
 *
 * Output ammo format is [URI+POST-style](https://yandextank.readthedocs.io/en/latest/tutorial.html?highlight=autostop#uri-post-style).
 */

/* tslint:disable:no-console */

import fs from 'fs';
import readline from 'readline';
import assert from 'assert';
import {URL} from 'url';
import {Counters} from 'app/lib/counters/update-counters-provider';
import {MockDataGenerator} from './lib/mock-data-generator';

const MAX_VALID_KEYS = 1000;
const INVALID_KEYS_PERCENT = 30;

interface RequestBody {
    ip: string;
    referrer?: string;
    key?: string;
    counters?: Counters;
}

const generator = new MockDataGenerator();

// Permanent ticket for tests.
const serviceTicket = fs.readFileSync('src/tests/stress/ticket').toString().trim();

console.log('[Connection: close]');
console.log('[User-Agent: tank]');
console.log('[Content-Type: application/json]');
console.log(`[X-Ya-Service-Ticket: ${serviceTicket}]`);

const rl = readline.createInterface({input: process.stdin});

rl.on('line', (line) => {
    // Skip first line
    if (line === 'request_body') {
        return;
    }

    let body: RequestBody;
    try {
        body = JSON.parse(line);
    } catch (err) {
        console.error(`Failed to parse line ${line} as JSON: ${err}`);
        return;
    }

    const bodyString = JSON.stringify({
        ip: generator.getRandomIpAddress(),
        referrer: mockReferrer(body.referrer),
        key: mockKey(body.key),
        counters: body.counters
    });
    const bodySize = Buffer.byteLength(bodyString);

    console.log(`${bodySize} /v1/check`);
    console.log(bodyString);
});

function mockReferrer(referrer?: string): string | undefined {
    if (!referrer) {
        return;
    }

    let url: URL;
    try {
        url = new URL(referrer);
    } catch (err) {
        console.error(`Failed to parse ${referrer} as URL: ${err}`);
        return;
    }

    // Mock host, but keep 'yandex.ru' for whitelist tests.
    if (!url.hostname.endsWith('yandex.ru')) {
        url.hostname = generator.getRandomHostname();
    }

    // Remove path and query, just in case they contain some personal data.
    url.pathname = '';
    url.search = '';

    return url.toString();
}

function mockKey(originalKey?: string): string | undefined {
    if (!originalKey) {
        return;
    }

    if (Math.random() * 100 < INVALID_KEYS_PERCENT) {
        return generator.getInvalidKey();
    }

    // Use hashing to generate same mock key for same original key.
    const hashCode = getHashCode(originalKey);
    assert(hashCode >= 0);

    return generator.getValidKey(hashCode % MAX_VALID_KEYS);
}

function getHashCode(str: string): number {
    let hash: number = 0;
    for (let i = 0; i < str.length; i++) {
        hash = (Math.imul(31, hash) + str.charCodeAt(i)) | 0;
    }

    // Convert to unsigned integer
    hash = hash >>> 0;

    return hash;
}
