package ru.yandex.market.mvp.strategy;

import androidx.annotation.Nullable;

import moxy.MvpView;
import moxy.viewstate.ViewCommand;

class AnotherDummyViewCommand extends ViewCommand<MvpView> {

    public AnotherDummyViewCommand() {
        super(null, null);
    }

    public AnotherDummyViewCommand(@Nullable final String tag) {
        super(tag, null);
    }

    @Override
    public void apply(final MvpView view) {
        // no-op
    }

    @Override
    public String toString() {
        return "AnotherDummyViewCommand[tag=" + getTag() + "]";
    }
}
