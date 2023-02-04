package ru.auto.tests.passport.account;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.Thread.currentThread;

/**
 * Created by vicdev on 14.08.17.
 */
public class AccountKeeper {

    private Map<Long, Optional<List<Account>>> accounts = newHashMap();

    public void add(Account account) {
        long threadId = currentThread().getId();
        if (accounts.containsKey(threadId)) {
            accounts.get(threadId).get().add(account);
        } else {
            accounts.put(threadId, Optional.of(newArrayList(account)));
        }
    }

    public List<Account> get() {
        long threadId = currentThread().getId();
        if (accounts.containsKey(threadId)) {
            return accounts.get(threadId).orElse(newArrayList());
        }
        return newArrayList();
    }

    public void clear() {
        long threadId = currentThread().getId();
        accounts.remove(threadId);
    }
}
