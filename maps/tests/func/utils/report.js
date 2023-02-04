const chalk = require('chalk');
const paintText = (text, color) => (color && chalk[color]) ? chalk[color](text) : text;

module.exports = class Report {
    constructor(opts) {
        this.rows = [];

        this._headText = opts.head;
        this._headTextColor = opts.color;
    }

    add(text) {
        this.rows = this.rows.concat(text);
        return this;
    }

    _createHeader() {
        const line = '─'.repeat(this._headText.length);
        const head = paintText(this._headText, this._headTextColor);

        return chalk.gray(
            '┌─' + line + '─┐\n' +
            '│ ' + head + ' │\n' +
            '└─' + line + '─┘'
        );
    }

    _createRows() {
        const indent = chalk.gray('  │');

        return this.rows
            .map((row) => `${indent}  ${row.replace(/\n/g, `\n${indent}`)}`)
            .join('\n');
    }

    toString() {
        const header = this._createHeader();
        const rows = this._createRows();

        return `${header}\n${rows}`;
    }
};
