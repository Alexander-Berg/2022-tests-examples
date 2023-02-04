export function random (min: number, max: number) {
    return min + Math.floor(Math.random() * (max - min + 1));
}

export function pseudoRandom(seed: number) {
    let value = seed;

    return function() {
        value = value * 16807 % 2147483647;

        return value;
    };
}
