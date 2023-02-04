import * as fs from 'fs';
export {nockUtils} from './nock-utils';

const tvmTicket = fs.readFileSync(`${__dirname}/../__fixtures__/tvm-ticket`).toString().trim();

export {tvmTicket};
