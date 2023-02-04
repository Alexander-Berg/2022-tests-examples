const getSellerTypeText = require('./getSellerTypeText');

it('должен вернуть "Частное лицо", если частник', () => {
    expect(getSellerTypeText({})).toEqual('Частное лицо');
});

it('должен вернуть "Официальный дилер", если официальный дилер и новая тачка', () => {
    expect(getSellerTypeText({ salon: { is_oficial: true }, section: 'new' })).toEqual('Официальный дилер');
});

it('должен вернуть "Автосалон", если seller_type=COMMERCIAL', () => {
    expect(getSellerTypeText({ seller_type: 'COMMERCIAL' })).toEqual('Автосалон');
});

it('должен вернуть "Профессиональный продавец", если в имени ID пользователя и есть encrypted_user_id', () => {
    expect(getSellerTypeText({
        additional_info: { other_offers_show_info: { encrypted_user_id: '123' } },
    })).toEqual('Профессиональный продавец');
});
