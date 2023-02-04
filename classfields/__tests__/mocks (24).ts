import pick from 'lodash/pick';

import { IMortgageProgramBank } from 'realty-core/types/mortgage/mortgageProgram';

export const getBank = (keys: string[]) => {
    const bank = {
        id: '1',
        name: 'Альфа-Банк',
        legalName: 'АО «Альфа-Банк»',
        licenseNumber: '1326',
        licenseDate: '2015-01-16',
        headOfficeAddress: 'Москва, ул. Каланчевская, 27',
    };

    return pick(bank, ['id', 'name', ...keys]) as IMortgageProgramBank;
};
