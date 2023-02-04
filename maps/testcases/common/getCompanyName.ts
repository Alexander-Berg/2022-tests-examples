import dotenv from 'dotenv';
import { first, isNil, keys } from 'lodash';
import { BrowserEnum } from './constants';
import { getBranchName } from '../../utils';

dotenv.config();

const COMPANY_NAME_PREFIX = `Y-Test`;
const currentDay = new Date();
const day = currentDay.getDate();
const month = currentDay.getMonth() + 1;
const year = currentDay.getFullYear();

const branchName = getBranchName({ orElse: process.env.TEST_RUN_BRANCH });
const stNumber = first(/\d+/.exec(branchName));

const dateText = `${month}.${day}.${year}`;

const COMPANY_NAME_REGEXP = new RegExp(
  // eslint-disable-next-line no-useless-escape
  `${COMPANY_NAME_PREFIX} ${stNumber} (${keys(BrowserEnum).join('|')}) \\d+ \\d+\.\\d+\.\\d+`,
);
const DAYS_UNTIL_SAVE_TEST_DATA = 3 * 24 * 60 * 60 * 1000;

export const getCompanyName = (testCaseId: string | number, browser: BrowserEnum): string => {
  return `${COMPANY_NAME_PREFIX} ${stNumber} ${browser} ${testCaseId} ${dateText}`;
};

export const checkIsTestCompanyName = (name: string): boolean => {
  return COMPANY_NAME_REGEXP.test(name);
};

export const getDateCreateCompanyFromName = (name: string): string | null => {
  return first(name.match(/\d+\.\d+\.\d+/)) ?? null;
};

export const checkIsOldCompany = (name: string): boolean => {
  const dateCreateStr = getDateCreateCompanyFromName(name);

  if (isNil(dateCreateStr)) {
    return false;
  }

  const date = new Date(dateCreateStr);

  return currentDay.getTime() - date.getTime() > DAYS_UNTIL_SAVE_TEST_DATA;
};
