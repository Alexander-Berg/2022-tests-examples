import * as fs from 'fs';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function loadFixture(name: string): any {
    return JSON.parse(fs.readFileSync(`src/tests/integration/fixtures/${name}`, 'utf8'));
}
