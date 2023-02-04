import React from 'react';
import { render } from '@testing-library/react';

import LazyImage from './LazyImage';

it('не упадет, если забыть src, отрендерит какую-то штучку', () => {
    render(<LazyImage src={ undefined as unknown as string }/>);
    const results = document.querySelectorAll('.LazyImage__previewContainer_fallback');

    expect(results).toHaveLength(1);
});
