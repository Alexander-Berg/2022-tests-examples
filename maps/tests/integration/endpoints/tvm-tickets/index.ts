// If you need new tickets then add them here and run `make generate-tvm-tickets-for-test-integration`

import * as fs from 'fs';
import {integrationTestsConfig, TvmClientName} from 'src/config';

export const SELF_SERVICE_TICKET = ticket('service', 'self');
export const TAKEOUT_SERVICE_TICKET = ticket('service', 'takeout');
export const PASSPORT_SERVICE_TICKET = ticket('service', 'passport');
export const UNKNOWN_SERVICE_TICKET = ticket('service', 'unknown');

export const UID1_USER_TICKET = ticket('user', 1);
export const UID2_USER_TICKET = ticket('user', 2);
export const UID3_USER_TICKET = ticket('user', 3);
export const UID999_USER_TICKET = ticket('user', 999);

function ticket(type: 'service', name: TvmClientName | 'unknown'): string;
function ticket(type: 'user', uid: number): string;
function ticket(type: 'service' | 'user', arg: string | number): string {
    const filename = `tests/integration/endpoints/tvm-tickets/${type}/${type === 'user' ? `uid${arg}` : arg}`;
    return module.parent ? readExistingTicket(filename) : generateNewTicket(filename, type, arg);
}

function readExistingTicket(filename: string): string {
    return fs.readFileSync(filename, 'utf8').trim();
}

function generateNewTicket(filename: string, type: string, arg: string | number) {
    const {execSync} = require('child_process') as typeof import('child_process');
    const run = (cmd: string) => {
        // tslint:disable-next-line: no-console
        console.log(`${cmd} ...`);
        execSync(cmd);
        // tslint:disable-next-line: no-console
        console.log(`\t ok`);
    };

    if (fs.existsSync(filename) && process.argv[2] !== '--force=true') {
        // tslint:disable-next-line: no-console
        console.log(`${filename} already exists`);
    } else {
        if (type === 'service') {
            const tvm = integrationTestsConfig['tvm.clients'];
            const src = arg === 'unknown' ? 999 : tvm[arg as TvmClientName];
            run(`ya tool tvmknife unittest service -s ${src} -d ${tvm.self} > ${filename}`);
        } else {
            run(`ya tool tvmknife unittest user -d ${arg} > ${filename}`);
        }
    }

    // If module is ran for generating tickets then nobody uses the exports anyway.
    return 'GENERATED';
}
