/* tslint:disable:no-console */

import {execFileSync} from 'child_process';
import {ArgumentParser, RawDescriptionHelpFormatter} from 'argparse';
import {endpointToAmmoFile, getAvailableEndpoints, getEndpointsFromArgs} from './utils';

const S3_BUCKET_NAME = 'constructor-int-ammo';

function testCapacity(endpoint: string, fireArgs: string[]): void {
    fire({
        endpoint,
        component: endpoint,
        configPath: 'tests/stress/capacity.yaml',
        fireArgs
    });
}

function testConst(endpoint: string, fireArgs: string[]): void {
    fire({
        endpoint,
        component: `${endpoint} const`,
        configPath: 'tests/stress/const.yaml',
        fireArgs
    });
}

interface FireOptions {
    configPath: string;
    endpoint: string;
    component: string;
    fireArgs: string[];
}

function fire({
    configPath,
    endpoint,
    component,
    fireArgs
}: Readonly<FireOptions>): void {
    const ammoFile = endpointToAmmoFile(endpoint);
    const ammoUrl = `http://${S3_BUCKET_NAME}.s3.mds.yandex.net/${ammoFile}`;
    execFileSync('node_modules/.bin/qtools',
        [
            'fire',
            '-e', 'stress',
            '-c', configPath,
            '-o', `phantom.ammofile=${ammoUrl}`,
            '-o', `uploader.component=constructor-int ${component}`,
            ...fireArgs
        ],
        {
            stdio: 'inherit'
        }
    );
}

function main(): void {
    const parser = new ArgumentParser({
        epilog: getAvailableEndpoints(),
        formatterClass: RawDescriptionHelpFormatter
    });
    parser.addArgument(['-e', '--endpoint'], {
        nargs: '+'
    });
    parser.addArgument(['-t', '--type'], {
        choices: ['capacity', 'const'],
        nargs: '+',
        required: true,
        dest: 'types'
    });

    const [args, fireArgs] = parser.parseKnownArgs();
    const endpoints = getEndpointsFromArgs(args);

    let hasErrors = false;

    for (const endpoint of endpoints) {
        if (args.types.includes('capacity')) {
            console.log(`Start regression test (capacity) for endpoint "${endpoint}"`);
            try {
                testCapacity(endpoint, fireArgs);
            } catch {
                // Don't print error, because child process redirects `stderr` to parent process.
                hasErrors = true;
            }
        }
        if (args.types.includes('const')) {
            console.log(`Start regression test (const) for endpoint "${endpoint}"`);
            try {
                testConst(endpoint, fireArgs);
            } catch {
                hasErrors = true;
            }
        }
    }

    process.exit(hasErrors ? 1 : 0);
}

main();
