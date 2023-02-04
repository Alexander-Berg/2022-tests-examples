class Calculate {
    constructor(data) {
        this.data = data;
    }

    stdDeviation() {
        let n = 0;
        let sum = 0;
        if (typeof this.average === 'undefined') {
            this.simpleAverage();
        }
        this.data.forEach((el) => {
            n++;
            sum += Math.pow(el - this.average, 2);
        });

        return n > 0 ? Math.sqrt(sum / n) : 0;
    }

    simpleAverage() {
        let n = 0;
        let sum = 0;
        this.data.forEach((el) => {
            n++;
            sum += el;
        });

        this.average = n > 0 ? sum / n : 0;

        return this.average;
    }

    median() {
        const middle = (this.data.length + 1) / 2;
        const sorted = [...this.data].sort((v1, v2) => v1 - v2);
        const isEven = sorted.length % 2 === 0;

        return isEven ? (sorted[middle - 1.5] + sorted[middle - 0.5]) / 2 : sorted[middle - 1];
    }

    percentile(per) {
        const sorted = [...this.data].sort((v1, v2) => v1 - v2);
        return sorted[Math.round((sorted.length - 1) * per)];
    }
}

module.exports = Calculate;
