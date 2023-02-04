package ru.auto.tests.desktop.mock;

import ru.auto.tests.commons.mountebank.http.predicates.PredicateType;
import ru.auto.tests.desktop.mock.beans.stub.Parameters;
import ru.auto.tests.desktop.mock.beans.stub.Predicate;

import static java.lang.String.format;
import static ru.auto.tests.desktop.mock.beans.stub.Predicate.predicate;

public class PredicateTypeDependentMethods {

    public static Predicate getPredicate(PredicateType predicateType, Parameters parameters) {
        Predicate predicate = predicate();

        switch (predicateType) {
            case EQUALS:
                predicate.setEquals(parameters);
                break;
            case DEEP_EQUALS:
                predicate.setDeepEquals(parameters);
                break;
            case MATCHES:
                predicate.setMatches(parameters);
                break;
            case CONTAINS:
                predicate.setContains(parameters);
                break;
            case STARTS_WITH:
                predicate.setStartsWith(parameters);
                break;
            case ENDS_WITH:
                predicate.setEndsWith(parameters);
                break;
            case EXISTS:
                predicate.setExists(parameters);
                break;
            default:
                throw new RuntimeException(format("Передан не поддерживаемый predicateType = «%s»", predicateType));
        }

        return predicate;
    }

    public static Parameters getParameters(PredicateType predicateType, Predicate predicate) {
        Parameters parameters;

        switch (predicateType) {
            case EQUALS:
                parameters = predicate.getEquals();
                break;
            case DEEP_EQUALS:
                parameters = predicate.getDeepEquals();
                break;
            case MATCHES:
                parameters = predicate.getMatches();
                break;
            case CONTAINS:
                parameters = predicate.getContains();
                break;
            case STARTS_WITH:
                parameters = predicate.getStartsWith();
                break;
            case ENDS_WITH:
                parameters = predicate.getEndsWith();
                break;
            case EXISTS:
                parameters = predicate.getExists();
                break;
            default:
                throw new RuntimeException(format("Передан не поддерживаемый predicateType = «%s»", predicateType));
        }

        if (parameters == null) {
            throw new RuntimeException(
                    format("Передан predicateType = «%s», которого нет в редактируемом JSON'e. " +
                            "Добавь правильный в withPredicateType()", predicateType));
        }

        return parameters;
    }

}
