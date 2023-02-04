function getRulerUrlOfLength(n: number): string {
    const tail = new Array(n - 1)
        .fill(null)
        .map((_, index) => {
            const x = Math.ceil(index / 4) * 0.01;
            switch (index % 4) {
                case 0:
                    return [x, x].join(',');
                case 1:
                    return [-x, x].join(',');
                case 2:
                    return [x, -x].join(',');
                case 3:
                    return [-x, -x].join(',');
            }
        })
        .join('~');
    return `?rl=37.53573142,55.71200736~${tail}`;
}

export {getRulerUrlOfLength};
