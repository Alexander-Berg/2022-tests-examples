/* eslint-disable no-console */

import { execFileSync } from 'child_process';
import { join } from 'path';
import assert from 'assert';
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import { updateStartrekIssue } from '@vertis/ci-tools';

type ArcDiffJson = Array<{
    names: Array<{
        path: string;
    }>;
}>

type PrDiffStat = Array<PrDiffStatEntry>

interface PrDiffStatEntry {
    deletions: number;
    insertions: number;
    testfile: boolean;
    path: string;
}

interface PrDiffSummary {
    deletions: number;
    insertions: number;
}

// начнем проверять PR, где меньше 100 строк изменений в КОДЕ (не в тестах)
const CHANGED_LOC_LIMIT = 100;

const {
    BASE_HASH,
    ISSUE_IDS,
    STARTREK_API_TOKEN,
} = process.env;
assert.ok(BASE_HASH, 'env.BASE_HASH is not defined');

// vcs_info.upstream_revision_hash vcs_info.merge_revision_hash
// arc diff --name-only <vcs_info.upstream_revision_hash>
// arc diff --name-only <vcs_info.upstream_revision_hash> <vcs_info.merge_revision_hash>

getPrDiffStat(BASE_HASH);
const prDiffStat = getPrDiffStat('HEAD^');
const hasTests = prDiffStat.some((item) => item.testfile);
// NOTE: тут возможно надо будет отсекать еще package-lock и что-то еще
const prDiffSummary = prDiffStat
    .filter(item => !item.testfile)
    .reduce<PrDiffSummary>((acc, item) => {
    acc.deletions += item.deletions;
    acc.insertions += item.insertions;
    return acc;
}, { deletions: 0, insertions: 0 });
console.log('PR diff summary', JSON.stringify(prDiffSummary), '\n');

let questionQaFlag = true;

if (!hasTests) {
    console.log(`This PR hasn't tests. "tested" tag will not be set. Keep calm and write tests!`);
    questionQaFlag = false;
}

const changedLoc = prDiffSummary.deletions + prDiffSummary.insertions;
if (changedLoc > CHANGED_LOC_LIMIT) {
    console.log(`Changed LOC=${ changedLoc } is more than limit=${ CHANGED_LOC_LIMIT }. "tested" tag will not be set`);
    questionQaFlag = false;
}

if (questionQaFlag) {
    console.log(`You're good buddy! "tested" tag will be set for your PR!`);
}

const startrekIssueIds: Array<string> = JSON.parse(ISSUE_IDS || '[]');
startrekIssueIds.forEach((issueId) => {
    updateStartrekIssue({
        token: STARTREK_API_TOKEN,
        issueId: issueId,
        tags: [ { name: 'question_qa', action: questionQaFlag ? updateStartrekIssue.TAG_ACTION.ADD : updateStartrekIssue.TAG_ACTION.REMOVE } ],
    }).catch((e: Error) => {
        console.error('FAILED TO UPDATE ISSUE', e);
    });
});

function getPrDiffStat(base: string): PrDiffStat {
    const arcRoot = execFileSync(
        'arc',
        [ 'root' ],
        { encoding: 'utf8' },
    ).trim();

    console.time('Changed files in PR');
    const changedFilesInPr = JSON.parse(
        execFileSync(
            'arc',
            [ 'diff', '--name-only', '--json', base, '../../..' ],
            { encoding: 'utf8' },
        ),
    ) as ArcDiffJson;
    console.timeEnd('Changed files in PR');
    console.log(`Changed files in PR (arc diff ${ base })`, JSON.stringify(changedFilesInPr, null, 2), '\n');

    console.time('PR diff statistics');
    const prDiffStat = changedFilesInPr[0].names.reduce<PrDiffStat>((res, entry) => {
        const diffStat: Array<string> = execFileSync(
            'arc',
            [ 'diff', '--stat', base, join(arcRoot, entry.path) ],
            { encoding: 'utf8' },
        ).trim().split('\n');

        const path = diffStat[0].split(' ')[0];
        const insertions = Number((diffStat[1].match(/(\d+) insertions/) || [])[1]);
        const deletions = Number((diffStat[1].match(/(\d+) deletions/) || [])[1]);
        const statEntry: PrDiffStatEntry = {
            path,
            insertions,
            deletions,
            testfile: isTestfile(entry.path),
        };
        res.push(statEntry);
        return res;
    }, []);
    console.timeEnd('PR diff statistics');
    console.log('PR diff statistics', JSON.stringify(prDiffStat, null, 2));

    return prDiffStat;
}

function isTestfile(pathname: string): boolean {
    return (
        pathname.endsWith('.mock.js') ||
        pathname.endsWith('.mock.ts') ||
        pathname.endsWith('.test.js') ||
        pathname.endsWith('.test.ts') ||
        pathname.endsWith('.test.tsx') ||
        pathname.endsWith('.browser.js') ||
        pathname.endsWith('.browser.tsx') ||
        pathname.includes('/__image_snapshots__/') || // jest-puppeteer-react screenshot
        pathname.includes('/__snapshots__/') // jest-snapshot
    );
}
