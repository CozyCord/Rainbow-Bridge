package net.cozystudios.rainbowbridge;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class TaskScheduler {

    private static final List<Task> TASKS = new ArrayList<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
    }

    public static void schedule(Runnable action, int ticks) {
        TASKS.add(new Task(ticks, action));
    }

    private static void tick() {
        Iterator<Task> it = TASKS.iterator();

        while (it.hasNext()) {
            Task t = it.next();
            t.ticks--;

            if (t.ticks <= 0) {
                t.action.run();
                it.remove();
            }
        }
    }

    private static class Task {
        int ticks;
        Runnable action;

        Task(int ticks, Runnable action) {
            this.ticks = ticks;
            this.action = action;
        }
    }
}
