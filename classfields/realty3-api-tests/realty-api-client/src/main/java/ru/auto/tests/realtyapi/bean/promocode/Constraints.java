package ru.auto.tests.realtyapi.bean.promocode;

import lombok.Data;
import lombok.experimental.Accessors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static java.util.concurrent.TimeUnit.MINUTES;

@Data
@Accessors(chain = true)
public class Constraints {
    private String deadline;
    private int totalActivations;
    private int userActivations;
    private String[] blacklist;

    public Constraints(int totalAct, int userAct) {
        this.setValidDeadline()
                .setTotalActivations(totalAct)
                .setUserActivations(userAct)
                .setBlacklist(new String[]{});
    }

    public Constraints setOutdatedDeadline() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
        Date date = new Date(System.currentTimeMillis() - MINUTES.toMillis(10));
        this.deadline = formatter.format(date);
        return this;
    }

    private Constraints setValidDeadline() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
        Date date = new Date(System.currentTimeMillis() + MINUTES.toMillis(10));
        this.deadline = formatter.format(date);
        return this;
    }

    public Constraints addOneUserToBlacklist(String uid) {
        ArrayList<String> blockedUid = new ArrayList<>(Arrays.asList(this.blacklist));
        blockedUid.add(uid);
        this.blacklist = blockedUid.toArray(new String[0]);
        return this;
    }
}
