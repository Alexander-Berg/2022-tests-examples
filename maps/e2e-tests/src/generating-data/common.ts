import fs from 'fs';
import path from 'path';

const CYPRESS_FIXTURE_PATH = path.join('cypress', 'fixtures');

export const writeToFixture = (name: string, data: {}): void => {
  fs.mkdirSync(CYPRESS_FIXTURE_PATH, { recursive: true });

  fs.writeFileSync(path.join(CYPRESS_FIXTURE_PATH, name), JSON.stringify(data, null, 2));
};

export const readFromFixture = <F extends {}>(name: string): F => {
  return JSON.parse(fs.readFileSync(path.join(CYPRESS_FIXTURE_PATH, name), { encoding: 'utf8' }));
};
