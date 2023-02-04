import dotenv from 'dotenv';
import { BrowserEnum } from './constants';
import { getBranchName } from '../../utils';

dotenv.config();

const branchName = getBranchName({ orElse: process.env.TEST_RUN_BRANCH });
const stNumber = /\d+/.exec(branchName)[0];

const TEST_USER_LOGIN_MASK = /yndx-\d+-(chrome|edge)-\d+/;

export const isTestLogin = (login: string): boolean => {
  return TEST_USER_LOGIN_MASK.test(login);
};

export const getLogin = (testCaseId: string | number, browser: BrowserEnum): string => {
  const login = `yndx-${stNumber}-${browser}-${testCaseId}`;

  if (!TEST_USER_LOGIN_MASK.test(login)) {
    throw new Error(`new login is invalid: ${login}`);
  }

  return login;
};
