import dotenv from 'dotenv';
import { map } from 'lodash';
import type { Version } from '@yandex-int/testpalm-api';
import got, { CancelableRequest, OptionsOfTextResponseBody, Response } from 'got';
import type { TestRunWrapT } from './testpalm/model';
import { getBranchName } from '../utils';

dotenv.config();

const send = (
  method: OptionsOfTextResponseBody['method'],
  path: string,
  body: OptionsOfTextResponseBody['json'],
): CancelableRequest<Response<string>> => {
  const options: OptionsOfTextResponseBody = {
    method,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `OAuth ${process.env.ST_TOKEN}`,
    },
    json: body,
  };

  return got(`https://st-api.yandex-team.ru/v2/${path}`, options);
};
const startrek = {
  createComment: (issue: string, text: string): CancelableRequest<Response<string>> =>
    send('POST', `issues/${issue}/comments?isAddToFollowers=false`, { text }),
};

const ONLY_DIGITS = /^\d+$/;

const getStartrekIssueId = (branchName: string): string | null => {
  if (!branchName) {
    return null;
  }

  const [, numberPart] = branchName.split('-');

  const isIssueId = ONLY_DIGITS.test(numberPart);

  if (!isIssueId) {
    return null;
  }

  return `BBGEO-${numberPart}`;
};

export const notifyInStarTrack = async (
  version: Version,
  testRuns: Array<TestRunWrapT>,
): Promise<void> => {
  const branchName = getBranchName({ orElse: process.env.TEST_RUN_BRANCH });

  const issueId = getStartrekIssueId(branchName);
  console.info('issueId:', issueId);
  if (!issueId) {
    throw new Error(`not found issue id from branch: ${branchName}`);
  }

  const notificatedIssueId = issueId; // lastAffectedIssueId || issueId;

  const comment = `
=== **Version**
https://testpalm.yandex-team.ru/courier/version/${encodeURI(version.id)}
=== **TestRuns**
${map(testRuns, ({ id }) => {
  return `- https://testpalm.yandex-team.ru/courier/testrun/${id}`;
}).join('\n')}
`;
  await startrek.createComment(notificatedIssueId, comment);
};
