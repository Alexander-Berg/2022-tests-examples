import * as fs from 'fs';

export const UID123_USER_TICKET = readTicket('user/uid123');

export const CORRECT_SRC_SERVICE_TICKET = readTicket('service/correct-src');
export const WRONG_SRC_SERVICE_TICKET = readTicket('service/wrong-src');

function readTicket(name: string): string {
    return fs.readFileSync(`src/tests/integration/tvm-tickets/${name}`, 'utf8').trim();
}
