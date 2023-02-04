import prepareSearchParametersForResource from './prepareSearchParametersForResource';

it('правильно подготавливает параметры для ресурса', () => {
    const result = prepareSearchParametersForResource({ km_age_from: 100 });

    expect(result).toMatchSnapshot();
});
