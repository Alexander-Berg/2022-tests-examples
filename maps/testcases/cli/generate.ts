import yargs from 'yargs';
import { ArgumentsT } from '../testpalm/model';
import { removeTestRuns } from '../commands/remove-test-runs';
import { generateTestRuns } from '../commands/generate-test-runs';

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
  .example([['npm run testruns:generate -- -e chrome', 'Generate test cases only for chrome env']])
  .argv as ArgumentsT;

(async (): Promise<void> => {
  await removeTestRuns(argv);
  await generateTestRuns(argv);
})();
