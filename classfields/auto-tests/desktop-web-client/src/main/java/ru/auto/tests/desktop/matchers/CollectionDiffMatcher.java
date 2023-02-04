package ru.auto.tests.desktop.matchers;

import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.collections.CollectionUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Collection;

import static java.util.stream.Collectors.toList;

/**
 * @author kurau (Yuri Kalinin)
 */
@Accessors(chain = true)
public class CollectionDiffMatcher extends TypeSafeMatcher<Collection> {

    private Collection actual;
    private Collection shouldBe;
    private Collection shouldNotBe;

    @Setter
    private Collection<Matcher> ignore;

    @Setter
    private Collection expected;

    @Override
    public void describeTo(Description description) {
        description.appendText("Ожидали элементы")
                .appendValueList("[", ", ", "]", expected);
    }

    @Override
    protected void describeMismatchSafely(Collection expected, Description mismatchDescription) {
        mismatchDescription.appendText("\n• в списке ").appendValueList("[", ", ", "]", actual)
                .appendText("\n• не хватает значений ").appendValueList("[", ", ", "]", shouldBe)
                .appendText("\n• лишние значения ").appendValueList("[", ", ", "]", shouldNotBe);
    }

    @Override
    protected boolean matchesSafely(Collection actual) {
        this.actual = actual;
        Collection commonPart = CollectionUtils.intersection(expected, actual);
        Collection diff = CollectionUtils.disjunction(expected, actual);
        Collection newDiff = (Collection) diff.stream()
                .filter(d -> ignore.stream().filter(m -> m.matches(d)).collect(toList()).isEmpty())
                .peek(e -> System.out.println(e))
                .collect(toList());
        shouldBe = CollectionUtils.intersection(expected, newDiff);
        shouldNotBe = CollectionUtils.intersection(actual, newDiff);

        ListsDiffData attachment = ListsDiffData.listsDiffData()
                .setCommon(commonPart).setExpected(shouldBe).setActual(shouldNotBe);

        if (newDiff.size() > 0) {
            new DefaultAttachmentProcessor().addAttachment(attachment,
                    new FreemarkerAttachmentRenderer("listdiff.ftl"));
        }
        return newDiff.size() == 0;
    }

    public static CollectionDiffMatcher sameCollection(Collection expected) {
        return new CollectionDiffMatcher().setExpected(expected);
    }
}
