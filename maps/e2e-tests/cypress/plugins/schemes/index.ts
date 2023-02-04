import { getAddRouteGenerateScheme } from './add-routes/add-route.schema';
import type { GenerateSchemaT } from 'generate-data';

export const schemes: Record<GenerateDataTypes, GenerateSchemaT<string>> = {
  'add-route': getAddRouteGenerateScheme(process.env.BRANCH_NAME),
};
