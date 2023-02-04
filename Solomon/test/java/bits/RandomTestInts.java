package ru.yandex.solomon.codec.bits;

import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.misc.random.Random2;

/**
 * @author Stepan Koltsov
 */
@ParametersAreNonnullByDefault
public class RandomTestInts {

    public static int randomIntForTest(Random random) {
        switch (random.nextInt(3)) {
            case 0: {
                return new Random2(random).randomElement(new int[] {
                    0,
                    1,
                    -1,
                    Integer.MAX_VALUE,
                    Integer.MIN_VALUE,
                });
            }
            case 1:
                return random.nextInt(100) - 200;
            case 2:
                return random.nextInt();
            default:
                throw new UnsupportedOperationException("unreachable");
        }
    }

    public static long randomLongForTest(Random random) {
        switch (random.nextInt(4)) {
            case 0: {
                return new Random2(random).randomElement(new long[] {
                    0,
                    1,
                    -1,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    Long.MIN_VALUE,
                    Long.MAX_VALUE,
                });
            }
            case 1:
                return (long) random.nextInt(100) - 200;
            case 2:
                return (long) random.nextInt();
            case 3:
                return random.nextLong();
            default:
                throw new UnsupportedOperationException("unreachable");
        }
    }

}
