import xlsx from 'xlsx';

import fetch from 'node-fetch';
import sha1 from 'crypto-js/sha1';
import { TaskTypeEnum } from 'utils/constants';
import { EXAMPLE_TASKS_URLS_BY_TYPE } from 'utils/hashes/constants';
import { forEach } from 'lodash';
import { getBaseUrl } from '../../utils/getBaseUrl';

const REQUEST_DELAY = 500;

const delay = (time: number): Promise<void> => {
  return new Promise(resolve => {
    setTimeout(resolve, time);
  });
};

export const downloadFile = async (fileUrl: string, retry = 3): Promise<ArrayBuffer> => {
  const baseUrl = getBaseUrl();

  const requestUrl = new URL(fileUrl, baseUrl);

  try {
    const response = await fetch(requestUrl);
    return await response.arrayBuffer();
  } catch (error) {
    if (retry) {
      console.info(`Retry. Attempts left: ${retry}`);
      await delay(REQUEST_DELAY);
      return downloadFile(fileUrl, retry - 1);
    }

    throw error;
  }
};

process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

export const getExampleTasksHashes = async (): Promise<Record<string, string>> => {
  const filePromises: Array<Promise<{ buffer: ArrayBuffer; type: string }>> = [];
  const hashes: Record<string, string> = {};

  forEach(TaskTypeEnum, type => {
    filePromises.push(
      downloadFile(EXAMPLE_TASKS_URLS_BY_TYPE[type]).then(buffer => ({ buffer, type })),
    );
  });
  const fileBuffersTypes = await Promise.all(filePromises);

  for (const { buffer, type } of fileBuffersTypes) {
    const content = new Uint8Array(buffer);
    const workbook = xlsx.read(content, { type: 'array' });
    const stringWorkbook = JSON.stringify(workbook);
    const hash = sha1(stringWorkbook).toString();

    hashes[type] = hash;
  }

  return hashes;
};
