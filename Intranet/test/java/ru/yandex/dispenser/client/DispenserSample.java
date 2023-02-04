package ru.yandex.dispenser.client;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ru.yandex.qe.dispenser.client.v1.Dispenser;

public final class DispenserSample {
    private Dispenser dispenser;

    public void sayDispenser() {
        System.out.println(dispenser);
    }

    public void setDispenser(@NotNull final Dispenser dispenser) {
        this.dispenser = dispenser;
    }

    public static void main(@NotNull final String[] args) {
        final ApplicationContext ctx = new ClassPathXmlApplicationContext("/spring/dispenser.xml");
        ctx.getBean(DispenserSample.class).sayDispenser();
        ctx.getBean(DispenserSample.class).sayDispenser();
    }
}
