jest.mock('auto-core/react/lib/isEnvBrowser', () => {
    return () => false;
});

const { signUrl, validateSign } = require('./signUrl');

it('должен правильно формировать подпись для URL', () => {
    const url = 'http://auto.ru';

    const sign = signUrl(url);

    expect(sign).toEqual('4iGaalGJC8navGf7uA2yrtfxiS92bKpsIMqjyrzKkwc');
});

it('должен отдавать "true" при валидации с правильной подписью', () => {
    const url = 'http://auto.ru';
    const sign = '4iGaalGJC8navGf7uA2yrtfxiS92bKpsIMqjyrzKkwc';

    const result = validateSign(url, sign);

    expect(result).toBe(true);
});

it('должен отдавать "false" при валидации с неправильной подписью', () => {
    const url = 'http://auto.ru';
    const sign = 'iAmBloodyCriminal';

    const result = validateSign(url, sign);

    expect(result).toBe(false);
});
