/* eslint-disable camelcase */
import {URL} from 'url';
import * as path from 'path';
import * as fs from 'fs';
import * as util from 'util';
import * as nock from 'nock';
import * as mkdirp from 'mkdirp';
import {expect} from 'chai';

const FIXTURES_DIR = 'src/tests/integration/fixtures';

export type StopRecording = () => void;

/**
 * Records responses, or plays responses from existing fixtures.
 *
 * If environment variable `NOCK_SAVE` is set and provided fixture `fixtureName` doesn't exist,
 * makes real HTTP requests and records responses. Otherwise, use recorded responses without making
 * HTTP requests.
 *
 * If environment variable `NOCK_UPDATE` is set, all fixtures will be updated.
 *
 * Returns `StopRecording` function, which must be called to write responses to fixtures, or check
 * that all previously recorded responses are used.
 */
export function startRecording(fixtureName: string): StopRecording {
    const fixturePath = path.join(FIXTURES_DIR, `${fixtureName}.json`);
    const needRecording = process.env.NOCK_UPDATE ||
        process.env.NOCK_SAVE && !fs.existsSync(fixturePath);
    let scopes: nock.Scope[] = [];

    if (needRecording) {
        nock.recorder.clear();
        nock.recorder.rec({
            dont_print: true,
            output_objects: true
        });
    } else {
        scopes = nock.load(fixturePath);
    }

    return () => {
        if (needRecording) {
            // Ignore requests to our service.
            // Cast play() result, because option `output_objects` for nock.recorder.rec() is used.
            const records = (nock.recorder.play() as nock.Definition[])
                .filter((fixture) => {
                    const url = new URL(fixture.scope);
                    return url.hostname !== '127.0.0.1' && url.hostname !== 'localhost';
                });

            mkdirp.sync(path.dirname(fixturePath));
            fs.writeFileSync(fixturePath, JSON.stringify(records, null, 4) + '\n');

            // Stop recording.
            nock.recorder.clear();
            nock.restore();
            //  Allow to intercepting HTTP calls again: https://github.com/nock/nock#activating
            nock.activate();
        } else {
            scopes.forEach((scope) => {
                expect(scope.isDone()).to.be.equal(
                    true,
                    util.format(
                        '%j was not used, consider removing %s to rerecord fixture',
                        scope.pendingMocks(),
                        fixturePath
                    )
                );
            });
        }
    };
}

export async function withRecording<T>(fixtureName: string, body: () => T | Promise<T>): Promise<T> {
    const stopRecording = startRecording(fixtureName);
    try {
        return await body();
    } finally {
        stopRecording();
    }
}
