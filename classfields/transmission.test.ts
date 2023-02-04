import transmission from './transmission';

it('должен правильно отдавать одну коробку', () => {
    expect(transmission.get('AUTOMATIC')).toBe('автоматическая');
});

it('должен правильно отдавать несколько коробок', () => {
    expect(transmission.get([ 'AUTOMATIC', 'VARIATOR' ])).toBe('автоматическая, вариатор');
});
