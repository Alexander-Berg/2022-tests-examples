package ru.yandex.solomon.expression.ast.serialization;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.solomon.expression.PositionRange;
import ru.yandex.solomon.expression.SelLexer;
import ru.yandex.solomon.expression.ast.Ast;
import ru.yandex.solomon.expression.ast.AstAnonymous;
import ru.yandex.solomon.expression.ast.AstAssignment;
import ru.yandex.solomon.expression.ast.AstBinOp;
import ru.yandex.solomon.expression.ast.AstCall;
import ru.yandex.solomon.expression.ast.AstIdent;
import ru.yandex.solomon.expression.ast.AstInterpolatedString;
import ru.yandex.solomon.expression.ast.AstOp;
import ru.yandex.solomon.expression.ast.AstSelector;
import ru.yandex.solomon.expression.ast.AstSelectors;
import ru.yandex.solomon.expression.ast.AstTernaryOp;
import ru.yandex.solomon.expression.ast.AstUnaryOp;
import ru.yandex.solomon.expression.ast.AstUse;
import ru.yandex.solomon.expression.ast.AstValue;
import ru.yandex.solomon.expression.ast.AstValueDouble;
import ru.yandex.solomon.expression.ast.AstValueDuration;
import ru.yandex.solomon.expression.ast.AstValueString;
import ru.yandex.solomon.labels.InterpolatedString;
import ru.yandex.solomon.labels.query.SelectorType;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
class RandomAst {

    private static final SelectorType[] PARSABLE_SELECTOR_TYPES = new SelectorType[] {
            SelectorType.GLOB,
            SelectorType.NOT_GLOB,
            SelectorType.EXACT,
            SelectorType.NOT_EXACT,
            SelectorType.REGEX,
            SelectorType.NOT_REGEX
    };

    private static int nextInt(Random random, int from, int to) {
        return from + random.nextInt(to - from);
    }

    private static String randomId(Random random) {
        while (true) {
            String id = RandomStringUtils.random(1, 0, 0, true, false, null, random) +
                    RandomStringUtils.random(nextInt(random, 1, 5), 0, 0, true, true, null, random);
            if (!SelLexer.KEYWORD_SET.contains(id)) {
                return id;
            }
        }
    }

    private static <T> T randomElement(Random random, T[] array) {
        return array[nextInt(random, 0, array.length)];
    }

    static PositionRange randomPositionRange(Random random) {
        if (random.nextBoolean()) {
            return PositionRange.UNKNOWN;
        }

        int lineBegin = nextInt(random,1, 11);
        int columnBegin = nextInt(random,1, 80);
        int offsetBegin = nextInt(random,0, 900);
        int lineEnd = nextInt(random,lineBegin, 11);
        int columnEnd = nextInt(random,lineBegin == lineEnd ? columnBegin : 1, 80);
        int offsetEnd = nextInt(random,offsetBegin + 1, 1000);

        return PositionRange.of(lineBegin, columnBegin, offsetBegin, lineEnd, columnEnd, offsetEnd);
    }

    static Ast randomExpr(Random random) {
        return randomExpr(random, 4);
    }

    static Ast randomExpr(Random random, int depth) {
        if (depth <= 0) {
            if (random.nextBoolean()) {
                return randomAstValue(random);
            } else {
                return randomSelectors(random);
            }
        }
        switch (random.nextInt(6)) {
            case 0: return randomAstValue(random);
            case 1: return randomUnaryOp(random, depth - 1);
            case 2: return randomBinOp(random, depth - 1);
            case 3: return randomTernOp(random, depth - 1);
            case 4: return randomCall(random, depth - 1);
            case 5: return randomSelectors(random);
        }
        throw new AssertionError("Unreachable");
    }

    static AstValue randomAstValue(Random random) {
        switch (random.nextInt(3)) {
            case 0: return new AstValueDouble(randomPositionRange(random), random.nextBoolean() ? random.nextInt(500) : random.nextDouble() * 500);
            case 1: return new AstValueString(randomPositionRange(random), randomId(random));
            case 2: return new AstValueDuration(randomPositionRange(random), Duration.ofSeconds(random.nextInt(1000000)));
        }
        throw new AssertionError("Unreachable");
    }

    static AstUnaryOp randomUnaryOp(Random random) {
        return randomUnaryOp(random, 4);
    }

    static AstUnaryOp randomUnaryOp(Random random, int depth) {
        String[] ops = new String[] {"+", "-"};
        AstOp op = new AstOp(randomPositionRange(random), randomElement(random, ops));
        return new AstUnaryOp(randomPositionRange(random), randomExpr(random, depth), op);
    }

    static AstBinOp randomBinOp(Random random) {
        return randomBinOp(random, 4);
    }

    static AstBinOp randomBinOp(Random random, int depth) {
        String[] ops = new String[] {"+", "-", "*", "/"};
        AstOp op = new AstOp(randomPositionRange(random), randomElement(random, ops));
        return new AstBinOp(randomPositionRange(random), randomExpr(random, depth), randomExpr(random, depth), op);
    }

    static AstTernaryOp randomTernOp(Random random) {
        return randomTernOp(random, 3);
    }

    static AstTernaryOp randomTernOp(Random random, int depth) {
        Ast cond = randomExpr(random, depth);
        Ast left = randomExpr(random, depth);
        Ast right = randomExpr(random, depth);
        return new AstTernaryOp(randomPositionRange(random), cond, left, right);
    }

    static AstIdent randomAstIdent(Random random) {
        return new AstIdent(randomPositionRange(random), randomId(random));
    }

    static AstCall randomCall(Random random) {
        return randomCall(random, 3);
    }

    static AstCall randomCall(Random random, int depth) {
        List<Ast> args = IntStream.range(0, nextInt(random,0, 4))
                .mapToObj(i -> randomExpr(random, depth - 1))
                .collect(Collectors.toList());
        return new AstCall(randomPositionRange(random), randomAstIdent(random), args);
    }

    static AstAnonymous randomAstAnon(Random random) {
        return new AstAnonymous(randomPositionRange(random), randomCall(random));
    }

    static AstAssignment randomAstAssign(Random random) {
        return new AstAssignment(randomPositionRange(random), randomId(random), randomCall(random));
    }


    private static InterpolatedString randomInterpolatedString(Random random) {
        String raw;
        do {
            raw = IntStream.range(3, 10)
                    .mapToObj(i -> {
                        if (random.nextBoolean()) {
                            return "{{" + randomId(random) + "}}";
                        } else {
                            return randomId(random);
                        }
                    })
                    .collect(Collectors.joining(" "));
        } while (!InterpolatedString.isInterpolatedString(raw));

        return InterpolatedString.parse(raw);
    }

    private static AstSelector randomSelector(Random random) {
        AstValueString key = new AstValueString(randomPositionRange(random), randomId(random));
        AstValue value;
        if (random.nextBoolean()) {
            value = new AstValueString(randomPositionRange(random), randomId(random));
        } else {
            value = new AstInterpolatedString(randomPositionRange(random), randomInterpolatedString(random));
        }
        return new AstSelector(randomPositionRange(random), key, value, randomElement(random, PARSABLE_SELECTOR_TYPES));
    }

    private static List<AstSelector> randomSelectorList(Random random) {
        return IntStream.range(0, nextInt(random,1, 4))
                .mapToObj(i -> randomSelector(random))
                .collect(Collectors.toList());
    }

    private static AstSelectors randomSelectors(Random random) {
        String name = random.nextBoolean() ? randomId(random) : "";
        return new AstSelectors(randomPositionRange(random), name, randomSelectorList(random));
    }

    static AstUse randomAstUse(Random random) {
        return new AstUse(randomPositionRange(random), randomSelectorList(random));
    }
}
