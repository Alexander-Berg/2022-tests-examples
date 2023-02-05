package ru.yandex.disk.provider;

import com.google.common.collect.Lists;
import ru.yandex.util.Path;

import java.util.List;

//TODO
public class FakeContentChangeNotifier extends ContentChangeNotifier {

    private final List<Path> changes = Lists.newArrayList();

    public FakeContentChangeNotifier() {
        super(null);
    }

    @Override
    public void notifyChange(Path path) {
        changes.add(path);
    }

    public List<Path> getChanges() {
        return changes;
    }
}
