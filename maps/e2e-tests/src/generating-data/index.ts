import { runGenerateDataBySchema, runRemoveData, writeTestData } from './data-generator';
import { currentSchema, commonCompaniesSchema } from './schema';

process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

// run generate test data
export const generateData = async (): Promise<void> => {
  await runGenerateDataBySchema(currentSchema, commonCompaniesSchema);
  await writeTestData();
};

export const removeData = async (): Promise<void> => {
  await runRemoveData(currentSchema);
};
