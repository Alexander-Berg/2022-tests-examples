import React from 'react';

import { fireEvent, render } from '@testing-library/react';

import { Collapse } from '.';

const TEST_ID = 'collapse';
const HEADER_TEST_ID = `${TEST_ID}-header`;
const CONTENT_TEST_ID = `${TEST_ID}-content`;

describe('Collapse', function () {
    it('should be default opened', function () {
        const { getByTestId } = render(<Collapse defaultOpen={true} testId={TEST_ID} />);

        const content = getByTestId(CONTENT_TEST_ID);

        expect(content.className).not.toContain('hidden');
    });

    it('should be default closed', function () {
        const { getByTestId } = render(<Collapse defaultOpen={false} testId={TEST_ID} />);

        const content = getByTestId(CONTENT_TEST_ID);

        expect(content.className).toContain('hidden');
    });

    it('should toggle state on header click', function () {
        const { getByTestId } = render(<Collapse defaultOpen={true} testId={TEST_ID} />);

        const header = getByTestId(HEADER_TEST_ID);
        const content = getByTestId(CONTENT_TEST_ID);

        fireEvent.click(header);
        expect(content.className).toContain('hidden');

        fireEvent.click(header);
        expect(content.className).not.toContain('hidden');
    });
});
