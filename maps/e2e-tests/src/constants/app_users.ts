import { times } from 'lodash';
import type { NewUserT } from 'generate-data';

export const appUsers = times(1001, (index): NewUserT => {
  return {
    login: `+79998877${index.toString().padStart(4, '0')}`,
    role: 'app',
  };
});
