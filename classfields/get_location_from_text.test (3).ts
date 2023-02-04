import get_location_from_text from './get_location_from_text';

it('должен вернуть широту и долготу из сообщения, содержащего урл на яндекс карты', () => {
    const text1 = 'https://yandex.ru/maps/?mode=whatshere&whatshere[point]=37.499173,55.755780&whatshere';
    expect(get_location_from_text(text1)).toStrictEqual({
        lat: 55.755780,
        lng: 37.499173,
    });
    const text2 = 'https://yandex.ru/maps/?mode=whatshere&whatshere%5Bpoint%5D=37.499173,55.755780&whatshere';
    expect(get_location_from_text(text2)).toStrictEqual({
        lat: 55.755780,
        lng: 37.499173,
    });
});

it('должен вернуть undefined, если в сообщении нет урла на яндекс карты или он неверного формата', () => {
    const text = 'Какое-т собщение https://yandex.ru/maps/';
    expect(get_location_from_text(text)).toBeUndefined();
});
