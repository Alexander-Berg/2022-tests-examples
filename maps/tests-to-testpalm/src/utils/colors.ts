const CI = process.env.CI;

const COLORS = {
    green: '\u001B[32m',
    yellow: '\u001B[33m',
    red: '\u001B[31m',
    reset: '\u001B[0m'
};

function color(input: string, color: keyof typeof COLORS): string {
    if (CI) {
        return input;
    }

    return `${COLORS[color]}${input}${COLORS.reset}`;
}

function green(input: string): string {
    return color(input, 'green');
}

function yellow(input: string): string {
    return color(input, 'yellow');
}

function red(input: string): string {
    return color(input, 'red');
}

export {red, green, yellow};
