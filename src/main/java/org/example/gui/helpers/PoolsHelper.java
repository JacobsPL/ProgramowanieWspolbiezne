package org.example.gui.helpers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.example.config.AppConfig;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class PoolsHelper {

    private static final int QUEUE_PADDING = 14;
    private static final int QUEUE_SPACING = 16;
    private static final int POOL_PADDING = 20;
    private static final int POOL_SPACING = 18;
    private static final int MIN_WAIT_IN_QUEUE_MS = 700;
    private static final int MAX_WAIT_IN_QUEUE_MS = 1500;

    private final Rectangle exitZone;
    private final AnchorPane animationPane;
    private final List<PoolState> pools;
    private final Map<Circle, PoolState> customerPools = new IdentityHashMap<>();
    private final Map<Circle, Integer> customerPoolSlots = new IdentityHashMap<>();
    private final Map<Circle, Integer> customerQueueSlots = new IdentityHashMap<>();

    public PoolsHelper(AnchorPane animationPane,
                       Rectangle olympicPoolZone,
                       Rectangle recreationalPoolZone,
                       Rectangle paddlingPoolZone,
                       Rectangle olympicWaitZone,
                       Rectangle recreationalWaitZone,
                       Rectangle paddlingWaitZone,
                       Rectangle exitZone) {
        AppConfig config = AppConfig.getInstance();
        this.animationPane = animationPane;
        this.exitZone = exitZone;
        this.pools = List.of(
                new PoolState(olympicPoolZone, olympicWaitZone, config.getInt("pool.olympic.capacity")),
                new PoolState(recreationalPoolZone, recreationalWaitZone, config.getInt("pool.recreational.capacity")),
                new PoolState(paddlingPoolZone, paddlingWaitZone, config.getInt("pool.paddling.capacity"))
        );
    }

    public void moveCustomerToPool(Circle customer, Rectangle pool) {
        PoolState poolState = findPoolState(pool);
        Timeline timeline = new Timeline();
        int delay = ThreadLocalRandom.current().nextInt(200, 500);

        KeyFrame keyFrame = new KeyFrame(
                Duration.millis(delay),
                event -> moveCustomerToPoolQueue(customer, poolState)
        );

        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    public Rectangle getRandomPool() {
        return pools.get(ThreadLocalRandom.current().nextInt(pools.size())).poolZone;
    }

    private void moveCustomerToPoolQueue(Circle customer, PoolState pool) {
        int queueSlot = reserveQueueSlot(pool);

        pool.waitingCustomers.add(customer);
        customerQueueSlots.put(customer, queueSlot);

        Point2D queuePosition = getQueuePosition(pool.waitZone, queueSlot);
        AnimationHelper.moveTo(customer, queuePosition, () -> waitInPoolQueue(pool));
    }

    private void waitInPoolQueue(PoolState pool) {
        Timeline queueWaitTime = new Timeline(
                new KeyFrame(
                        Duration.millis(ThreadLocalRandom.current().nextInt(MIN_WAIT_IN_QUEUE_MS, MAX_WAIT_IN_QUEUE_MS)),
                        event -> tryMoveNextCustomerIntoPool(pool)
                )
        );
        queueWaitTime.play();
    }

    private void tryMoveNextCustomerIntoPool(PoolState pool) {
        while (pool.occupiedCustomers < pool.capacity && !pool.waitingCustomers.isEmpty()) {
            Circle customer = pool.waitingCustomers.remove(0);
            Integer queueSlot = customerQueueSlots.remove(customer);
            if (queueSlot != null) {
                pool.occupiedQueueSlots[queueSlot] = false;
            }

            int slot = reservePoolSlot(pool);
            pool.occupiedCustomers++;
            customerPools.put(customer, pool);
            customerPoolSlots.put(customer, slot);

            Point2D poolPosition = getPoolPosition(pool, slot);
            AnimationHelper.moveTo(customer, poolPosition, () -> waitInPool(customer));
        }
    }

    private void waitInPool(Circle customer) {
        Timeline swimmingTime = new Timeline(
                new KeyFrame(
                        Duration.millis(ThreadLocalRandom.current().nextInt(1000, 3000)),
                        event -> leavePool(customer)
                )
        );
        swimmingTime.play();
    }

    private void leavePool(Circle customer) {
        PoolState pool = customerPools.remove(customer);
        Integer slot = customerPoolSlots.remove(customer);

        if (pool != null && slot != null) {
            pool.occupiedSlots[slot] = false;
            pool.occupiedCustomers--;
            tryMoveNextCustomerIntoPool(pool);
        }

        if (shouldVisitAnotherPool()) {
            moveCustomerToPool(customer, getRandomPool());
        } else {
            moveCustomerToExit(customer);
        }
    }

    private boolean shouldVisitAnotherPool() {
        return ThreadLocalRandom.current().nextInt(100) < 60;
    }

    public void moveCustomerToExit(Circle customer) {
        Timeline timeline = new Timeline();
        int delay = ThreadLocalRandom.current().nextInt(200, 500);

        KeyFrame keyFrame = new KeyFrame(
                Duration.millis(delay),
                event -> transitionToExit(customer)
        );

        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    private void transitionToExit(Circle customer) {
        Point2D exitPosition = AnimationHelper.centerOf(exitZone);
        AnimationHelper.moveTo(customer, exitPosition, () -> animationPane.getChildren().remove(customer));
    }

    private Point2D getQueuePosition(Rectangle waitZone, int index) {
        double startX = waitZone.getLayoutX() + QUEUE_PADDING;
        double startY = waitZone.getLayoutY() + QUEUE_PADDING;

        int columns = (int) ((waitZone.getWidth() - 2 * QUEUE_PADDING) / QUEUE_SPACING);
        if (columns <= 0) {
            columns = 1;
        }

        double x = startX + (index % columns) * QUEUE_SPACING;
        double y = startY + (index / columns) * QUEUE_SPACING;

        return new Point2D(x, y);
    }

    private static int getQueueSlotCount(Rectangle waitZone) {
        int columns = (int) ((waitZone.getWidth() - 2 * QUEUE_PADDING) / QUEUE_SPACING);
        int rows = (int) ((waitZone.getHeight() - 2 * QUEUE_PADDING) / QUEUE_SPACING);

        if (columns <= 0) {
            columns = 1;
        }

        if (rows <= 0) {
            rows = 1;
        }

        return columns * rows;
    }

    private Point2D getPoolPosition(PoolState pool, int slot) {
        double startX = pool.poolZone.getLayoutX() + POOL_PADDING;
        double startY = pool.poolZone.getLayoutY() + POOL_PADDING;

        int columns = (int) ((pool.poolZone.getWidth() - 2 * POOL_PADDING) / POOL_SPACING);
        if (columns <= 0) {
            columns = 1;
        }

        double x = startX + (slot % columns) * POOL_SPACING;
        double y = startY + (slot / columns) * POOL_SPACING;

        return new Point2D(x, y);
    }

    private int reservePoolSlot(PoolState pool) {
        for (int i = 0; i < pool.occupiedSlots.length; i++) {
            if (!pool.occupiedSlots[i]) {
                pool.occupiedSlots[i] = true;
                return i;
            }
        }
        return 0;
    }

    private int reserveQueueSlot(PoolState pool) {
        for (int i = 0; i < pool.occupiedQueueSlots.length; i++) {
            if (!pool.occupiedQueueSlots[i]) {
                pool.occupiedQueueSlots[i] = true;
                return i;
            }
        }
        return pool.occupiedQueueSlots.length - 1;
    }

    private PoolState findPoolState(Rectangle pool) {
        for (PoolState poolState : pools) {
            if (poolState.poolZone == pool) {
                return poolState;
            }
        }
        return pools.get(0);
    }

    private static class PoolState {
        private final Rectangle poolZone;
        private final Rectangle waitZone;
        private final int capacity;
        private final boolean[] occupiedSlots;
        private final boolean[] occupiedQueueSlots;
        private final List<Circle> waitingCustomers = new ArrayList<>();
        private int occupiedCustomers = 0;

        private PoolState(Rectangle poolZone, Rectangle waitZone, int capacity) {
            this.poolZone = poolZone;
            this.waitZone = waitZone;
            this.capacity = capacity;
            this.occupiedSlots = new boolean[capacity];
            this.occupiedQueueSlots = new boolean[getQueueSlotCount(waitZone)];
        }
    }
}
