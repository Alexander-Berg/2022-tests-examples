import React from 'react';
//import { screen } from '@testing-library/react';

import { renderComponent } from 'www-poffer/react/utils/testUtils';

import BankFrameOsagoSravni from './BankFrameOsagoSravni';

const LICENCE_PLATE = 'A123BC000';

it('должен добавить ГРЗ в скрипт', async() => {
    await renderComponent(<BankFrameOsagoSravni licenseplate={ LICENCE_PLATE }/>);
    const attr = document.getElementsByTagName('script')[0].getAttribute('data-platenumber');
    expect(attr).toBe(LICENCE_PLATE);
});

it('не должен добавить ГРЗ в скрипт', async() => {
    await renderComponent(<BankFrameOsagoSravni/>);
    const attr = document.getElementsByTagName('script')[0].getAttribute('data-platenumber');
    expect(attr).toBe(null); // getAttribute возвращает null на несуществующие атрибуты
});
