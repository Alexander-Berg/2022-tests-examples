import getFormattedMoscowDate from './getFormattedMoscowDate';

it('форматнет дату для московской таймзоны', () => {
    const result = getFormattedMoscowDate(1587243600000);
    expect(result).toEqual('19 апреля 2020');
});
