package ru.yandex.market.mvp.strategy;

import androidx.annotation.Nullable;

import moxy.MvpView;
import moxy.viewstate.ViewCommand;

class DummyViewCommand extends ViewCommand<MvpView> {

    public DummyViewCommand() {
        super(null, null);
    }

    public DummyViewCommand(@Nullable final String tag) {
        super(tag, null);
    }

    @Override
    public void apply(final MvpView view) {
        // no-op
    }

    @Override
    public String toString() {
        return "DummyViewCommand[tag=" + getTag() + "]";
    }
}
