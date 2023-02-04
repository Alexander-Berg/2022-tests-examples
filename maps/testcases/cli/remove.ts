import yargs from 'yargs';
import { ArgumentsT } from '../testpalm/model';
import { removeTestRuns } from '../commands/remove-test-runs';

const argv = yargs
  .options({
    environments: {
      alias: 'e',
      array: true,
      type: 'string',
      description: 'Browser environments for test runs',
      default: ['chrome', 'edge'],
    },
  })
  .example([['npm run testruns:remove -- -e chrome', 'Remove test cases only for chrome env']])
  .argv as ArgumentsT;

removeTestRuns(argv);
