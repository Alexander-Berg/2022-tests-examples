import {Project} from 'app/lib/keys/service-project-provider';

const VALID_KEY_TEMPLATE = 'aaaaaaaa-aaaa-aaaa-aaaa-000000000000';
const INVALID_KEY = 'ffffffff-ffff-ffff-ffff-ffffffffffff';

const HOSTNAMES = [
    'example.ru',
    'maps.example.ru',
    'example.com',
    'maps.example.com',
    'example.net',
    'maps.example.net',
    'localhost'
];

const IP_PREFIXES = [
    '127.0.0.',
    '10.10.10.',
    '2021:db00::',
    'cafe:ff00::'
];
const IP_LIST = IP_PREFIXES.map((prefix) => prefix + '0/24');

// Get random integer in [0, max) range
function getRandomInt(max: number) {
    return Math.floor(Math.random() * max);
}

export class MockDataGenerator {
    getRandomIpAddress(): string {
        const ipPrefix = IP_PREFIXES[getRandomInt(IP_PREFIXES.length)];
        return `${ipPrefix}${getRandomInt(256)}`;
    }

    getRandomHostname(): string {
        return HOSTNAMES[getRandomInt(HOSTNAMES.length)];
    }

    getValidKey(index: number): string {
        const suffix = index.toString(16);
        const prefixLength = VALID_KEY_TEMPLATE.length - suffix.length;
        const prefix = VALID_KEY_TEMPLATE.substr(0, prefixLength);
        return prefix + suffix;
    }

    getInvalidKey(): string {
        return INVALID_KEY;
    }

    private _getValidKeysList(count: number): string[] {
        const keys: string[] = [];
        for (let index = 0; index < count; ++index) {
            keys.push(this.getValidKey(index));
        }
        return keys;
    }

    private _getProject(key: string, index: number): Project {
        return {
            project_id: `project_${index.toString()}`,
            tariff: 'apimaps_free',
            keys: [
                {
                    key: key,
                    active: true,
                    custom_params: {
                        // Make 10% of keys contain referrer restrictions
                        http_referer_list: Math.random() * 100 < 10 ? HOSTNAMES : undefined,

                        // Make 5% of keys contain IP restrictions
                        ip_list: Math.random() * 100 < 5 ? IP_LIST : undefined
                    }
                }
            ],
            hidden: false,
            limits: {},
            update_dt: new Date().toISOString()
        };
    }

    getProjects(count: number): Project[] {
        return this._getValidKeysList(count).map((key, index) => this._getProject(key, index));
    }
}
