package ru.yandex.solomon.expression.expr.op.bin;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.solomon.expression.exceptions.CompilerException;
import ru.yandex.solomon.expression.test.ForEachSelVersionRunner;
import ru.yandex.solomon.expression.test.SelCompilerTestBase;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.value.SelValueBoolean;
import ru.yandex.solomon.expression.value.SelValueDouble;
import ru.yandex.solomon.expression.value.SelValueString;
import ru.yandex.solomon.expression.version.SelVersion;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
@RunWith(ForEachSelVersionRunner.class)
public class CompareOpTest extends SelCompilerTestBase {

    public CompareOpTest(SelVersion version) {
        super(version);
    }

    private static class TestCompares<T extends SelValue> extends SelCompilerTestBase {
        TestCompares(SelVersion version) {
            super(version);
        }

        <U extends Comparable<U>> void comparesLike(T firstSelVal, T secondSelVal, U firstValue, U secondValue) {
            int cmp = firstValue.compareTo(secondValue);
            comparesTo(firstSelVal, secondSelVal, cmp < 0, cmp <= 0, cmp > 0, cmp >= 0, cmp == 0, cmp != 0);
        }
        void comparesTo(T firstSelVal, T secondSelVal, boolean lt, boolean le, boolean gt, boolean ge, boolean eq, boolean ne) {
            testEval(SelValueBoolean.of(lt), "a <  b", "a", firstSelVal, "b", secondSelVal);
            testEval(SelValueBoolean.of(le), "a <= b", "a", firstSelVal, "b", secondSelVal);
            testEval(SelValueBoolean.of(gt), "a >  b", "a", firstSelVal, "b", secondSelVal);
            testEval(SelValueBoolean.of(ge), "a >= b", "a", firstSelVal, "b", secondSelVal);
            testEval(SelValueBoolean.of(eq), "a == b", "a", firstSelVal, "b", secondSelVal);
            testEval(SelValueBoolean.of(ne), "a != b", "a", firstSelVal, "b", secondSelVal);
        }
    }

    private SelValueDouble of(double a) {
        return new SelValueDouble(a);
    }

    private SelValueString of(String a) {
        return new SelValueString(a);
    }

    @Test
    public void compareDoubles() {
        TestCompares<SelValueDouble> tester = new TestCompares<>(version);

        for (double [] pair : new double [][] {{41, 42}, {0, 42}, {42, 42}, {42, 0}, {-10, 10}}) {
            tester.comparesLike(of(pair[0]), of(pair[1]), pair[0], pair[1]);
        }

        // Sel compares NaNs by regular < and >, thus not consistent with Comparable<Double>
        tester.comparesTo(of(5), of(Double.NaN), false, false, false, false, false, true);
        tester.comparesTo(of(Double.NaN), of(Double.NaN), false, false, false, false, false, true);
    }

    @Test
    public void compareStrings() {
        TestCompares<SelValueString> tester = new TestCompares<>(version);

        for (String [] pair : new String [][] {{"", ""}, {"", "foo"}, {"foo", "bar"}, {"foo", "foobar"}, {"X", "x"}, {"Alice", "Алиса"}, {"Алиса", "Боб"}}) {
            tester.comparesLike(of(pair[0]), of(pair[1]), pair[0], pair[1]);
        }
    }

    @Test(expected = CompilerException.class)
    public void compareIncomparable() {
        testEval(SelValueBoolean.FALSE, "1 < 'one'");
    }
}
