package ru.auto.tests.commons.extension.listener;

import io.qameta.atlas.core.util.MethodInfo;
import lombok.Getter;
import org.hamcrest.Matcher;
import ru.auto.tests.commons.extension.interfaces.MethodFormat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StepTitleContainer {

    public static final Map<String, MethodFormat> loggableMethods = new HashMap<>();

    static {
        loggableMethods.put("click", (elementName, args) -> String.format("Кликаем на элемент «%s»", elementName));
        loggableMethods.put("hover", (elementName, args) -> String.format("Наводим на элемент «%s»", elementName));
        loggableMethods.put("submit", (elementName, args) -> String.format("Нажимаем на элемент «%s»", elementName));
        loggableMethods.put("clear", (elementName, args) -> String.format("Очищаем элемент «%s»", elementName));
        loggableMethods.put("sendKeys", (elementName, args) -> {
            String arguments = Arrays.toString(((CharSequence[]) args[0]));
            return String.format("Вводим в элемент «%s» значение [%s]", elementName, arguments);
        });
        loggableMethods.put("waitUntil", (elementName, args) -> {
            Matcher matcher = (Matcher) (args[0] instanceof Matcher ? args[0] : args[1]);
            return String.format("Ждем пока элемент «%s» будет в состоянии [%s]", elementName, matcher);
        });
        loggableMethods.put("should", (elementName, args) -> {
            Matcher matcher = (Matcher) (args[0] instanceof Matcher ? args[0] : args[1]);
            return String.format("Проверяем что элемент «%s» в состоянии [%s]", elementName, matcher);
        });
    }

    public static String stepArgs(MethodInfo method) {
        Object[] args = method.getArgs();
        if (method.getMethod().getName().equals("sendKeys"))  {
            return " " + Arrays.toString(((CharSequence[]) args[0]));
        }
        if (method.getMethod().getName().equals("waitUntil")) {
            return " " + (args[0] instanceof Matcher ? args[0] : args[1]);
        }
        if (method.getMethod().getName().equals("should")) {
            return " " + (args[0] instanceof Matcher ? args[0] : args[1]);
        }
        return "";
    }

    public static String stepArgs(String action, Object[] args) {
        if (!Optional.ofNullable(args).isPresent()) {
            return "";
        }
        if (action.equals("sendKeys"))  {
            return " " + Arrays.toString(((CharSequence[]) args[0]));
        }
        if (action.equals("waitUntil")) {
            return " " + (args[0] instanceof Matcher ? args[0] : args[1]);
        }
        if (action.equals("should")) {
            return " " + (args[0] instanceof Matcher ? args[0] : args[1]);
        }
        return "";
    }

    public static Optional<MethodFormat> getStepTitle(String method) {
        return method.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(loggableMethods.get(method));
    }

}
