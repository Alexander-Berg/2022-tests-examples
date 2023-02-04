import { getAccounts } from 'constants/accounts';
import getCurrentBranchName from 'utils/getCurrentBranchName';
import { NewCompanyT } from 'generate-data';

const accounts = getAccounts();

const branchName = getCurrentBranchName();

export const newCompany: NewCompanyT = {
  name: `Грибное Королевство-${branchName}`,
  initial_login: accounts.admin,
  sms_enabled: true,
  mark_delivered_enabled: false,
  services: [
    {
      enabled: true,
      name: 'courier',
    },
    {
      enabled: true,
      name: 'mvrp',
    },
  ],
};
