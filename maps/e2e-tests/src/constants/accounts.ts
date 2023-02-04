import { getShortBranchName } from '../utils/getCurrentBranchName';

export const SUPER_USER_LOGIN = 'yndx-courier-test-su-manual';

export const getAccounts = (branchNameProps?: string): AccountsT => {
  const branchName = branchNameProps || getShortBranchName();

  return {
    superuser: SUPER_USER_LOGIN,

    adminAddRoute: `yndx-a-addRoute-${branchName}`,
    managerAddRoute: `yndx-m-addRoute-${branchName}`,

    admin: `yndx-admin-${branchName}`,
    adminMulti: `yndx-admin-multi-${branchName}`,
    manager: `yndx-manager-${branchName}`,
    managerEmpty: `yndx-empty-mngr-${branchName}`,
    managerMulti: `yndx-multi-mngr-${branchName}`,
    dispatcher: `yndx-dispatcher-${branchName}`,
    temp: `yndx-temp-mnl-${branchName}`,
    companyAadmin: `yndx-A-admin-${branchName}`,
    companyBadmin: `yndx-B-admin-${branchName}`,
    companyBmanager: `yndx-B-mngr-${branchName}`,
    companyVadmin: `yndx-V-admin-${branchName}`,
    companyVmanager: `yndx-V-mngr-${branchName}`,
    companyGadmin: `yndx-G-admin-${branchName}`,
    companyGmanager: `yndx-G-mngr-${branchName}`,
    companyDadmin: `yndx-D-admin-${branchName}`,
    companyDmanager: `yndx-D-mngr-${branchName}`,
    companyEadmin: `yndx-E-admin-${branchName}`,
    companyEmanager: `yndx-E-mngr-${branchName}`,

    testCompanyManager0: `yndx-manager_0-${branchName}`,
    testCompanyManager1: `yndx-manager_1-${branchName}`,
    testCompanyManager2: `yndx-manager_2-${branchName}`,
    testCompanyManager3: `yndx-manager_3-${branchName}`,
    testCompanyManager4: `yndx-manager_4-${branchName}`,
    testCompanyManager5: `yndx-manager_5-${branchName}`,
    testCompanyManager6: `yndx-manager_6-${branchName}`,
    testCompanyManager7: `yndx-manager_7-${branchName}`,
    testCompanyManager8: `yndx-manager_8-${branchName}`,

    mvrpManager: `yndx-mvrp-mngr-${branchName}`,

    mvrpViewManager: `yndx-mvrpV-mngr-${branchName}`,

    refBookManager: `yndx-refbk-mng-${branchName}`,

    exportRoutesAdmin: `yndx-exp-rts-adm-${branchName}`,
    exportRoutesManager: `yndx-exp-rts-mng-${branchName}`,

    appRoleAdmin: `yndx-a-app-role`,
    appRoleManager: `yndx-m-app-role`,
  };
};
