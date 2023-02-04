import React, { useCallback, useContext, useRef, useState } from 'react';

import { securityReporter } from '@trust/dom-observable';
import { dom } from '@trust/utils/dom';
import { Text } from '@yandex-lego/components/Text/desktop/bundle';
import block from 'bem-cn';

import { resultContext } from '../../../../store/result';

import './styles.css';

const defaultWhitelist = ['https://trust.yandex.ru', 'https://trust-test.yandex.ru'];

if (__DEV__) {
    defaultWhitelist.push('https://trust.local.yandex.ru:3000/');
}

const defaultHtmlFrame = `<div>
  <iframe src="https://yandex.ru"></iframe>
</div>`;

const b = block('frame-csp');

export const FrameCSP = (): JSX.Element => {
    const htmlFrameRef = useRef<HTMLTextAreaElement>();
    const [isCSPSet, setCSP] = useState(false);

    let htmlBlockFrame: HTMLElement;

    const resultCtx = useContext(resultContext);

    const updateFrame = useCallback(() => {
        if (htmlBlockFrame) {
            dom.remove(htmlBlockFrame);
        }

        htmlBlockFrame = document.createElement('div');
        htmlBlockFrame.innerHTML += htmlFrameRef.current.value;

        dom.add(document.body, htmlBlockFrame);
    }, []);

    const onSecurityReport = useCallback(
        (node: HTMLIFrameElement) => {
            resultCtx.append({
                event: {
                    frameSrc: node.src,
                },
            });
            resultCtx.start();
        },
        [resultCtx],
    );

    const updateCSP = useCallback(() => {
        if (!isCSPSet) {
            const reporterParams = { env: 'testing', reqId: '0' };

            if (__DEV__) {
                reporterParams.env = 'development';
            }

            securityReporter(defaultWhitelist, reporterParams, onSecurityReport);
            setCSP(true);
        }
    }, []);

    return (
        <div className={b()}>
            <Text as="h2" typography="headline-l" weight="bold">
                Тестирование CSP
            </Text>

            <div className={b('csp-settings')}>
                <button type="button" onClick={updateCSP} disabled={isCSPSet}>
                    Добавить валидатор фреймов на страницу
                </button>
            </div>

            <Text typography="headline-s">Whitelist:</Text>

            <div className={b('whitelist')}>{defaultWhitelist.join('\n')}</div>

            <Text typography="headline-s">Встраиваемый блок:</Text>

            <textarea ref={htmlFrameRef} defaultValue={defaultHtmlFrame} />

            <div className={b('csp-apply')}>
                <button type="button" onClick={updateFrame}>
                    Apply
                </button>
            </div>
        </div>
    );
};
