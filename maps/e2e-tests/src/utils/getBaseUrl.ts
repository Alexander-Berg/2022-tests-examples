import dotenv from 'dotenv';
import { getTestStandLink, getPreparedBranchName } from '../../../tools/utils';
import getCurrentBranchName from './getCurrentBranchName';

dotenv.config();

const IS_DEVELOPMENT = process.env.NODE_ENV === 'development';
const isLocalRun = process.env.LOCAL_TEST;

export const getBaseUrl = (): string => {
  if (isLocalRun) {
    return 'https://localhost.msup.yandex.ru:8082/courier/';
  }

  let standLink = process.env.BASE_URL as string;

  if (IS_DEVELOPMENT) {
    const branchName = getCurrentBranchName(); // or any your branchName
    const preparedBranchName = getPreparedBranchName(branchName);
    standLink = getTestStandLink(preparedBranchName);
  }

  return standLink;
};
