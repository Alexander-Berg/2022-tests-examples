import {execFileSync} from 'child_process';

const PROXY = 'hahn.yt.yandex.net';

export function createTable(path: string, attributes: string): void {
    execFileSync('ya', ['tool', 'yt', 'create',
        '--proxy', PROXY,
        '--type', 'table',
        '--path', path,
        '--attributes', attributes,
        '--recursive'
    ]);
}

export function writeTable(tablePath: string, rows: readonly any[]): void {
    execFileSync('ya', ['tool', 'yt', 'write', tablePath, '--format', 'json', '--proxy', PROXY], {
        input: rows.map((row) => JSON.stringify(row)).join('\n')
    });
}

export function eraseTable(tablePath: string): void {
    execFileSync('ya', ['tool', 'yt', 'erase', tablePath, '--proxy', PROXY], {
        env: {
            ...process.env,
            // Disable INFO messages
            // https://wiki.yandex-team.ru/yt/userdoc/faq/#qkakovyurovnilogirovanijavkonsolnomklienteikakonizadajutsja
            YT_LOG_LEVEL: 'ERROR'
        }
    });
}

/**
 * Removes Cypress node by the given path.
 */
export function remove(path: string): void {
    execFileSync('ya', ['tool', 'yt', 'remove',
        '--proxy', PROXY,
        '--path', path,
        '--recursive',
        '--force'
    ]);
}
