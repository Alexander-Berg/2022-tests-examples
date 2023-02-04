import React from 'react';
import cn from 'classnames';

import Button from 'auto-core/react/components/islands/Button/Button';
import Link from 'auto-core/react/components/islands/Link/Link';

import './TestId.css';

interface TestIdProps {
    className?: string;
    isActive?: boolean;
    action: () => void;
    testId: string;
    title: string;
}

export function TestId({ action, className, isActive = false, testId, title }: TestIdProps) {
    const buttonView = isActive ? Button.COLOR.WHITE_HOVER_BLUE_LIGHT_EXTRA : Button.COLOR.YELLOW;
    const buttonText = isActive ? 'Выйти' : 'Залипнуть';

    return (
        <div className={ cn('TestId', className) }>
            <Button className="TestId__button" size={ Button.SIZE.M } color={ buttonView } onClick={ action }>
                { buttonText }
            </Button>
            <div className="TestId__text">
                <Link className="TestId__link" url={ `https://ab.yandex-team.ru/testid/${ testId }` } target="_blank">
                    [{ testId }]:
                </Link><span className="TestId__title">{ title }</span>
            </div>
        </div>

    );
}
