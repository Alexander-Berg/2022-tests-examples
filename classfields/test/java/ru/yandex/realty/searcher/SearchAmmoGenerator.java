package ru.yandex.realty.searcher;

import org.apache.commons.lang.StringUtils;
import ru.yandex.common.util.collections.Cf;
import ru.yandex.common.util.collections.Cu;
import ru.yandex.realty.misc.enums.EnumResolver;
import ru.yandex.realty.model.offer.CategoryType;
import ru.yandex.realty.model.offer.OfferType;
import ru.yandex.realty.search.common.request.domain.SearchQuery;
import ru.yandex.realty.util.Range;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author aherman
 */
public class SearchAmmoGenerator {
    private static final String URL_PART = ":36201/search?";

    private static Random random = new Random();

    private static final Set<String> TESTABLE_FIELDS = Cf.set(
            "page",
            "pageSize",
            "searchSortOrder",
            "price",
            "rooms",
            "floor",
            "area",
            "livingSpace",
            "kitchenSpace"
    );


    private static final Map<String, MinMax> MIN_MAX = Cf.newHashMap();

    static {
        MIN_MAX.put("page", new MinMax(true, 0, 100));
        MIN_MAX.put("pageSize", new MinMax(true, 10, 100));
        MIN_MAX.put("rooms", new MinMax(true, 1, 4));
        MIN_MAX.put("floor", new MinMax(true, 1, 10));
        MIN_MAX.put("price", new MinMax(false, 1000, 10000000));
        MIN_MAX.put("area", new MinMax(false, 0, 250));
        MIN_MAX.put("livingSpace", new MinMax(false, 0, 250));
        MIN_MAX.put("kitchenSpace", new MinMax(false, 0, 30));
    }

    private static final Map<String, Generator> GENERATORS = Cf.newHashMap();

    static {
        GENERATORS.put("type", new EnumGenerator(OfferType.R, "type"));
        GENERATORS.put("category", new EnumGenerator(CategoryType.R, "category"));

        Field[] declaredFields = SearchQuery.class.getDeclaredFields();

        for (Field declaredField : declaredFields) {
            if (TESTABLE_FIELDS.contains(declaredField.getName())) {
                GENERATORS.put(declaredField.getName(), getGenerator(declaredField));
            }
        }
    }

    public static void main(String[] argv) {
        int totalUrls = Integer.parseInt(argv[0]);
        for (int i = 0; i < totalUrls; i++) {
            System.out.println(generateUrl());
        }
    }

    private static String generateUrl() {
        List<String> fields = Cf.newArrayList();
        fields.add("type");
        fields.add("category");

        fields.addAll(addRandomFields());

        List<String> parameters = Cf.newArrayList();
        for (String field : fields) {
            Generator generator = GENERATORS.get(field);
            parameters.add(generator.getValue());
        }
        return URL_PART + StringUtils.join(parameters, '&');
    }

    private static Collection<? extends String> addRandomFields() {
        List<String> shuffledFields = Cf.newArrayList(TESTABLE_FIELDS);
        Collections.shuffle(shuffledFields, random);
        int size = random.nextInt(shuffledFields.size());
        return Cu.take(size, shuffledFields);
    }

    private static Generator getGenerator(Field field) {
        String fieldName = field.getName();
        Class<?> fieldType = field.getType();
        if (Enum.class.isAssignableFrom(fieldType)) {
            Class<Enum> type = (Class<Enum>) fieldType;
            return new EnumGenerator(EnumResolver.er(type), fieldName);
        }

        if (Range.class.isAssignableFrom(fieldType)) {
            return new RangeGenerator(fieldName);
        }

        if (Boolean.class == fieldType || Boolean.TYPE == fieldType) {
            return new BooleanGenerator(fieldName);
        }

        if (Number.class.isAssignableFrom(fieldType) || fieldType.isPrimitive()) {
            return new NumericGenerator(fieldName);
        }

        if ("rooms".equals(fieldName)) {
            return new RoomsGenerator();
        }
        throw new IllegalArgumentException(field.getName());
    }

    private static abstract class Generator {
        protected String fieldName;

        public Generator(String fieldName) {
            this.fieldName = fieldName;
        }

        public abstract String getValue();
    }

    private static class EnumGenerator extends Generator {
        private final Enum[] values;

        public EnumGenerator(EnumResolver resolver, String fieldName) {
            super(fieldName);
            this.values = (Enum[]) resolver.knownValues().toArray(new Enum[0]);
        }

        @Override
        public String getValue() {
            int position = random.nextInt(values.length);
            return fieldName + "=" + values[position].name();
        }
    }

    private static class RangeGenerator extends Generator {
        private static final float DEFAULT_MIN = 0;
        private static final float DEFAULT_RANGE = 100000000;

        private final float MIN;
        private final float RANGE;

        public RangeGenerator(String fieldName) {
            super(fieldName);
            MinMax minMax = MIN_MAX.get(fieldName);
            if (minMax != null) {
                MIN = minMax.min;
                RANGE = minMax.range;
            } else {
                MIN = DEFAULT_MIN;
                RANGE = DEFAULT_RANGE;
            }
        }

        @Override
        public String getValue() {
            int configuration = random.nextInt(3);
            String result = "";
            float min = MIN;
            float range = RANGE;

            switch (configuration) {
                case 1:
                    result = String.format("%sMin=%.2f", fieldName, MIN + range * random.nextFloat());
                    break;

                case 0:
                    range = RANGE * random.nextFloat();
                    min = MIN + range;
                    result = String.format("%sMin=%.2f", fieldName, min);
                    // fall-through
                case 2:
                    range = (RANGE - range) * random.nextFloat();
                    result += String.format("&%sMax=%.2f", fieldName, min + range);
            }
            return result;
        }
    }

    private static class NumericGenerator extends Generator {
        private final float DEFAULT_MIN = 0;
        private final float DEFAULT_RANGE = 1000;

        private final float MIN;
        private final float RANGE;
        private final boolean IS_INTEGER;

        public NumericGenerator(String fieldName) {
            super(fieldName);
            MinMax minMax = MIN_MAX.get(fieldName);
            if (minMax != null) {
                MIN = minMax.min;
                RANGE = minMax.range;
                IS_INTEGER = minMax.isInt;
            } else {
                MIN = DEFAULT_MIN;
                RANGE = DEFAULT_RANGE;
                IS_INTEGER = false;
            }
        }

        @Override
        public String getValue() {
            float value = MIN + RANGE * random.nextFloat();
            if (IS_INTEGER) {
                return String.format("%s=%d", fieldName, (int)value);
            } else {
                return String.format("%s=%.2f", fieldName, value);
            }
        }
    }

    private static class BooleanGenerator extends Generator {

        public BooleanGenerator(String fieldName) {
            super(fieldName);
        }

        @Override
        public String getValue() {
            return String.format("%s=%s", fieldName, random.nextBoolean() ? "YES" : "NO");
        }
    }

    private static class RoomsGenerator extends Generator {

        public RoomsGenerator() {
            super("rooms");
        }

        @Override
        public String getValue() {
            return String.format("%s=%s", fieldName, random.nextInt(8));
        }
    }


    private static class MinMax {
        boolean isInt;
        float min;
        float range;

        private MinMax(boolean anInt, float min, float range) {
            isInt = anInt;
            this.min = min;
            this.range = range;
        }
    }
}
