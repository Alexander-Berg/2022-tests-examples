import salon from '../mocks/salon.mock';

import shouldRenderOnModerationIcon from './shouldRenderOnModerationIcon';

it('должен вернуть true, если поменялся номер телефона', () => {
    expect(shouldRenderOnModerationIcon('phones', salon as any)).toBe(true);
});
