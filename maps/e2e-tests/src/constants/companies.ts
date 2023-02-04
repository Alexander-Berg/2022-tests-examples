import getCurrentBranchName from '../utils/getCurrentBranchName';

const branchName = getCurrentBranchName(); // or any your branchName

export enum CompanyEnum {
  A = 'A',
  B = 'B',
  V = 'V',
  G = 'G',
  D = 'D',
  E = 'E',
  shareFromOthersCompany = 'shareFromOthersCompany',
}

export const companyNameRecord: Record<CompanyEnum, string> = {
  A: `Компания А-${branchName}`,
  B: `Компания Б-${branchName}`,
  V: `Компания В-${branchName}`,
  G: `Компания Г-${branchName}`,
  D: `Компания Д-${branchName}`,
  E: `Компания E-${branchName}`,
  shareFromOthersCompany: `Наблюдательная компания-${branchName}`,
};

export enum additionalCompanyEnum {
  someNumbers1 = 'someNumbers1',
  someNumbers2 = 'someNumbers2',
  someNumbers3 = 'someNumbers3',
  someLowerCase = 'someLowerCase',
  someUpperCase = 'someUpperCase',
  someUpperLowerCase = 'someUpperLowerCase',
  someEnglishLowerCase = 'someEnglishLowerCase',
  someEnglishUpperCase = 'someEnglishUpperCase',
  someRussian = 'someRussian',
}

const testPrefix = 'testCompany-';

export const additionalCompaniesNames: Record<additionalCompanyEnum, string> = {
  someNumbers1: `${testPrefix}1234567890-${branchName}`,
  someNumbers2: `${testPrefix}12345678901-${branchName}`,
  someNumbers3: `${testPrefix}12345678902-${branchName}`,
  someLowerCase: `${testPrefix}тестовая компания-${branchName}`,
  someUpperCase: `${testPrefix}ТЕСТОВАЯ КОМПАНИЯ-${branchName}`,
  someUpperLowerCase: `${testPrefix}ТЕСТОВАЯ компания-${branchName}`,
  someEnglishLowerCase: `${testPrefix}Mega test company-${branchName}`,
  someEnglishUpperCase: `${testPrefix}MEGA TEST THING-${branchName}`,
  someRussian: `${testPrefix}ЛогистикаF6Ц-${branchName}`,
};
