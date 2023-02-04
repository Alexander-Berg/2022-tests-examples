import React from 'react';
import { renderHook, act } from '@testing-library/react-hooks';

import useContent from './useContent';

it('запоминает и сбрасывает contentRef и scrollOnNextStep', () => {
    const {
        rerender,
        result,
    } = renderHook(() => useContent());

    const ref = React.createRef();

    act(() => {
        result.current.registerContent({
            scrollOnNextStep: true,
            contentRef: ref,
        });
    });

    rerender(true);

    expect(result.current.scrollOnNextStep).toBe(true);
    expect(result.current.contentRef).toBe(ref);

    act(() => {
        result.current.unregisterContent();
    });

    rerender(true);

    expect(result.current.scrollOnNextStep).toBe(false);
    expect(result.current.contentRef).toBeNull();
});
