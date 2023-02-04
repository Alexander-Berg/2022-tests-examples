const getName = require('./getSellerName');

it('должен вернуть имя продавца', () => {
    expect(getName({ seller: { name: 'Имя продавца' } })).toEqual('Имя продавца');
});

it('должен вернуть "Частное лицо", если в имени ID пользователя', () => {
    expect(getName({ seller: { name: 'id123456' } })).toEqual('Частное лицо');
});

it('должен вернуть "Профессиональный продавец", если в имени ID пользователя и есть encrypted_user_id', () => {
    expect(getName({
        seller: { name: 'id123456' },
        additional_info: { other_offers_show_info: { encrypted_user_id: '123' } },
    })).toEqual('Профессиональный продавец');
});
