package ru.yandex.disk.provider;

import ru.yandex.util.Path;

//TODO
public class ContentChangeNotifierStub extends ContentChangeNotifier {

    public ContentChangeNotifierStub() {
        super(null);
    }

    @Override
    public void notifyChange(Path directory) {
        //do nothing
    }
}
