from typing import Any, Callable, Generic, TypeVar

from hamcrest.core.base_matcher import BaseMatcher
from hamcrest.core.description import Description

T = TypeVar('T')


class AfterProcessingMatcher(BaseMatcher, Generic[T]):
    def __init__(self, processor: Callable[[Any], T], submatcher: BaseMatcher):
        self.processor = processor
        self.submatcher = submatcher

    def _matches(self, item: Any) -> bool:
        try:
            processed = self.processor(item)
        except Exception:
            return False
        return self.submatcher._matches(processed)

    def describe_mismatch(self, item: Any, mismatch_description: Description) -> None:
        try:
            processed = self.processor(item)
        except Exception as e:
            mismatch_description.append_description_of(item)                          \
                                .append_text(' failed to be processed by processor ') \
                                .append_description_of(self.processor)                \
                                .append_text(' with the exception ')                  \
                                .append_description_of(e)
        else:
            self.submatcher.describe_mismatch(processed, mismatch_description)

    def describe_to(self, description: Description) -> None:
        return self.submatcher.describe_to(description)


def convert_then_match(processor: Callable[[Any], T], matcher: BaseMatcher) -> AfterProcessingMatcher[T]:
    return AfterProcessingMatcher(processor, matcher)
