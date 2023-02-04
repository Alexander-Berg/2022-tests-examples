import imageMock from 'auto-core/react/dataDomain/mag/imageMock';

import getSrcSetMagImage from './getSrcSetMagImage';

it('должен правильно сформировать srcSet для всех размеров', () => {
    expect(getSrcSetMagImage(imageMock.value().sizes)).toMatchSnapshot();
});
