import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { TagSelect } from '../';

const mockTags = [
    { value: 'v1', label: 'label1' },
    { value: 'v2', label: 'label2' },
    { value: 'v3', label: 'label3' }
];

class SelectComponent extends React.Component {
    state = { value: undefined };

    handleChange = value => this.setState({ value });

    render() {
        return (
            <TagSelect
                tags={mockTags}
                value={this.state.value}
                onChange={this.handleChange}
                {...this.props}
            />
        );
    }
}

describe('TagSelect', () => {
    it('should render tag select', async() => {
        await render(
            <SelectComponent />,
            { viewport: { width: 350, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render disabled tag select', async() => {
        await render(
            <SelectComponent disabled />,
            { viewport: { width: 350, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render tag select with selected option 2', async() => {
        await render(
            <SelectComponent />,
            { viewport: { width: 350, height: 100 } }
        );

        await page.click('[data-test=tag-select-option-v2]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
