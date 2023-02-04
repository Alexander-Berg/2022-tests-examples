import relatedSelfPhoneValidator from './relatedSelfPhoneValidator';

it('кидает ошибку при совпадении номера с основным', () => {
    expect(() => {
        relatedSelfPhoneValidator('+79876543211', '+79876543212').validateSync('+79876543211');
    }).toThrow('Номер не должен совпадать с основным');
});

it('кидает ошибку при совпадении номера с рабочим', () => {
    expect(() => {
        relatedSelfPhoneValidator('+79876543211', '+79876543212').validateSync('+79876543212');
    }).toThrow('Номер не должен совпадать с рабочим');
});

it('не кидает ошибку при несовпадении номеров', () => {
    expect(() => {
        relatedSelfPhoneValidator('+79876543211', '+79876543212').validateSync('+79876543213');
    }).not.toThrow();
});
