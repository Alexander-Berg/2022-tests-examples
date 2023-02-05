import * as path from 'path';
import * as fs from 'fs';
import getStandUrl from './lib/get-stand-url';
import {Size, Theme} from '../src/types/custom';
import params from '../webpack/params';

interface InitialData {
    id: number;
    size: Size;
    theme: Theme;
}

const DEFAULT_OPTIONS: InitialData = {
    id: 1203061165,
    size: 's',
    theme: 'light'
};

const SIZE_MAPPING: Record<Size, string> = {
    s: 'маленький',
    m: 'средний',
    x: 'с комментариями'
};

const THEME_MAPPING: Record<Theme, string> = {
    light: 'светлая',
    dark: 'темная'
};

const rootDir = path.join(__dirname, '..');

async function generateTester(data: InitialData): Promise<string> {
    const sizeOptions = Object.entries(SIZE_MAPPING).map(([key, value]) =>
        `<option value="${key}"${value === data.size ? ' selected' : ''}>${value}</option>`
    );
    const themeOptions = Object.entries(THEME_MAPPING).map(([key, value]) =>
        `<option value="${key}"${value === data.theme ? ' selected' : ''}>${value}</option>`
    );
    const baseUrl = `${await getStandUrl({useCrowd: true})}${params.basePath}`;
    return (`
        <!DOCTYPE html>
        <html>
            <head>
                <meta charset="utf-8">
                <style>
                    .wrapper {padding: 40px; border-radius: 8px; text-align: center}
                    .control {margin: 12px 0;}
                    .control + .control {margin-left: 12px;}
                    #frame {width: 150px; height: 104px; resize: both; overflow: auto; border: 1px solid #e6e6e6;}
                </style>
                <script defer>
                    window.badgeOptions = {
                        size: '${data.size}',
                        orgId: '${data.id}',
                        theme: '${data.theme}'
                    };
                    window.updateUrl = function() {
                        const frame = document.getElementById('frame');
                        const options = window.badgeOptions;
                        const query = 'size=' + options.size + '&theme=' + options.theme;
                        frame.src = '${baseUrl}' + options.orgId + '/?' + query;
                        frame.style.borderRadius = (options.size === 'x' ? 8 : 6) + 'px';
                    }
                    document.addEventListener('DOMContentLoaded', function () {
                        const sizeSelect = document.getElementById('size');
                        sizeSelect.addEventListener('change', function (e) {
                            window.badgeOptions.size = e.currentTarget.value;
                        });
                        const themeSelect = document.getElementById('theme');
                        themeSelect.addEventListener('change', function (e) {
                            window.badgeOptions.theme = e.currentTarget.value;
                        });
                        const idInput = document.getElementById('id');
                        idInput.addEventListener('change', function (e) {
                            window.badgeOptions.orgId = e.currentTarget.value;
                        });
                        const refreshButton = document.getElementById('refresh');
                        refreshButton.addEventListener('click', window.updateUrl);

                        window.updateUrl();
                    });
                </script>
            </head>
            <body>
                <div class="wrapper">
                    <div>
                         <select class="control" id="size">${sizeOptions}</select>
                         <select class="control" id="theme">${themeOptions}</select>
                         <input class="control" id="id" value="${data.id}" />
                         <button class="control" id="refresh">REFRESH</button>
                    </div>
                    <iframe id="frame" src="about:blank"></iframe>
                </div>
            </body>
        </html>
    `);
}

async function writeReport(): Promise<void> {
    const dirPath = `reports/html-tester/`;
    const filePath = dirPath + 'index.html';
    fs.mkdirSync(path.join(rootDir, dirPath), {recursive: true});
    fs.writeFileSync(path.join(rootDir, filePath), await generateTester(DEFAULT_OPTIONS), {encoding: 'utf8'});
    console.log(`Report file has been written to ${filePath}.`);
}

writeReport();
