import employmentPhoneValidator from './employmentPhoneValidator';

it('кидает ошибку при совпадении номера с основным', () => {
    expect(() => {
        employmentPhoneValidator('+79876543211', '+79876543212').validateSync('+79876543211');
    }).toThrow('Номер не должен совпадать с основным');
});

it('кидает ошибку при совпадении номера с дополнительным', () => {
    expect(() => {
        employmentPhoneValidator('+79876543211', '+79876543212').validateSync('+79876543212');
    }).toThrow('Номер не должен совпадать с дополнительным');
});

it('не кидает ошибку при несовпадении номеров', () => {
    expect(() => {
        employmentPhoneValidator('+79876543211', '+79876543212').validateSync('+79876543213');
    }).not.toThrow();
});
