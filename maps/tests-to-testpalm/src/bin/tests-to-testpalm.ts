#!/usr/bin/env node

import * as path from 'path';
import * as yargs from 'yargs';

import run, {RunParams} from '../handlers/run';

yargs
    .help()
    .demandCommand()
    .command({
        command: 'sync',
        describe: 'Sync your tests with test palm',
        builder: {
            config: {
                alias: 'c',
                describe: 'Path to configuration.'
            },
            mode: {
                choices: ['full', 'dry-run', 'validation', 'pr'],
                default: 'dry-run',
                describe: 'Mode to run.'
            }
        },
        handler: (args: yargs.Arguments<{config: string; mode?: RunParams['mode']}>) => {
            run({
                configPath: path.join(process.cwd(), args.config),
                mode: args.mode ?? 'dry-run'
            }).catch((error) => {
                console.error(error);
                process.exit(1);
            });
        }
    })
    .strict()
    .fail((message, err) => {
        if (err) {
            process.exit(1);
        }

        // Error from yargs.
        // http://yargs.js.org/docs/#api-failfn
        // tslint:disable-next-line:no-console
        console.log(yargs.help());
        console.error(message);
        process.exit(1);
    })
    .wrap(yargs.terminalWidth())
    .parse();
