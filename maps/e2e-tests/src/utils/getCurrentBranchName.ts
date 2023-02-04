import last from 'lodash/last';
import dotenv from 'dotenv';

dotenv.config();

const { NODE_ENV } = process.env;

const MAX_BRANCH_NAME_LENGTH = 13;
const IS_DEVELOPMENT = NODE_ENV !== 'production';

const getBranchName = (): string => {
  const { BRANCH_NAME } = process.env;

  if (BRANCH_NAME) {
    return BRANCH_NAME;
  }

  throw new Error('not define process.env.BRANCH_NAME');
};

const getCurrentBranchName = (): string => {
  return getBranchName();
};

const BBGEO_PREFIX_REGEX = /^BBGEO-/i;

export const getShortBranchName = (branchName = getCurrentBranchName()): string => {
  const unprefixed = branchName.replace(BBGEO_PREFIX_REGEX, '').replace(/_/g, '-');
  const branchNameArr = unprefixed.toLowerCase().split('-');

  const branchNumber = branchNameArr[0];
  let branchNameLastWord = last(branchNameArr) as string;

  if (branchNameArr.length === 1) {
    return branchNumber;
  }

  let alreadyLength = branchNumber.length + branchNameLastWord.length;
  const firstRegExp = new RegExp(`^${branchNumber}`, 'i');
  const lastRegExp = new RegExp(`^${branchNameLastWord}`, 'i');

  if (alreadyLength > MAX_BRANCH_NAME_LENGTH) {
    branchNameLastWord = branchNameLastWord.slice(alreadyLength - MAX_BRANCH_NAME_LENGTH);
    alreadyLength = MAX_BRANCH_NAME_LENGTH;
  }

  const middleShortBranchName = unprefixed
    .replace(/-/g, '')
    .replace(firstRegExp, '')
    .replace(lastRegExp, '')
    .slice(0, MAX_BRANCH_NAME_LENGTH - alreadyLength);

  return `${branchNumber}${middleShortBranchName}${branchNameLastWord}`;
};

export default getCurrentBranchName;
