import { execSync } from 'child_process';
import dotenv from 'dotenv';

dotenv.config();

const run = async (): Promise<void> => {
  let error = null;

  try {
    console.info('generate e2e data');
    execSync('npm run e2e:generate-data', { stdio: 'inherit' });
  } catch (error) {
    console.info(error.toString());
    process.exit(1);
  }

  try {
    console.info('start e2e');
    execSync('npm run cypress-run:wrap', { stdio: 'inherit' });
  } catch (e) {
    error = e;
  }

  try {
    console.info('make e2e report');
    execSync('npm run generate-report', { stdio: 'inherit' });
  } catch (error) {
    console.info(error.toString());
    process.exit(1);
  }

  console.info('remove e2e data');
  execSync('npm run e2e:remove-data', { stdio: 'inherit' });

  if (error) {
    console.info(error.toString());
    process.exit(1);
  }

  process.exit(0);
};

run();
