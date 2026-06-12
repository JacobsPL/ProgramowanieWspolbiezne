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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class PoolsHelper {

    private static final int PADDING = 14;
    private static final int SPACING = 16;
    private final Rectangle exitZone;
    private final AnchorPane animationPane;
    private final Map<String, PoolViewState> pools = new HashMap<>();
    private final Map<Circle, PoolViewState> customerPools = new IdentityHashMap<>();
    private final Map<Circle, Integer> customerPoolSlots = new IdentityHashMap<>();
    private final Map<Circle, Integer> customerQueueSlots = new IdentityHashMap<>();
    private final Map<Circle, PoolSlot> pendingPoolSlotsToRelease = new IdentityHashMap<>();

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

        addPool(
                config.getString("pool.olympic.name"),
                olympicPoolZone,
                olympicWaitZone,
                config.getInt("pool.olympic.capacity")
        );
        addPool(
                config.getString("pool.recreational.name"),
                recreationalPoolZone,
                recreationalWaitZone,
                config.getInt("pool.recreational.capacity")
        );
        addPool(
                config.getString("pool.paddling.name"),
                paddlingPoolZone,
                paddlingWaitZone,
                config.getInt("pool.paddling.capacity")
        );
    }

    private void addPool(String poolName, Rectangle poolZone, Rectangle waitZone, int poolCapacity) {
        pools.put(poolName, new PoolViewState(poolZone, waitZone, poolCapacity));
    }

    public void moveCustomerToPoolQueue(Circle customer, String poolName, Runnable onFinished) {
        PoolViewState pool = findPoolState(poolName);
        int queueSlot = reserveSlot(pool, false);

        customerQueueSlots.put(customer, queueSlot);

        Point2D queuePosition = getPosition(pool, queueSlot,false);
        moveWithShortDelay(customer, queuePosition, () -> {
            releasePendingPoolSlot(customer);
            if (onFinished != null) {
                onFinished.run();
            }
        });
    }

    public void moveCustomerToPool(Circle customer, String poolName, Runnable onFinished) {
        PoolViewState pool = findPoolState(poolName);
        releaseQueueSlot(customer, pool);

        int poolSlot = reserveSlot(pool,true);
        customerPools.put(customer, pool);
        customerPoolSlots.put(customer, poolSlot);

        Point2D poolPosition = getPosition(pool, poolSlot,true);
        moveWithShortDelay(customer, poolPosition, onFinished);
    }

    public void leavePool(Circle customer, String poolName) {
        PoolViewState pool = customerPools.remove(customer);
        if (pool == null) {
            pool = findPoolState(poolName);
        }

        Integer poolSlot = customerPoolSlots.remove(customer);
        if (poolSlot != null) {
            pendingPoolSlotsToRelease.put(customer, new PoolSlot(pool, poolSlot));
        }
    }

    private void releasePendingPoolSlot(Circle customer) {
        PoolSlot poolSlot = pendingPoolSlotsToRelease.remove(customer);
        if (poolSlot != null) {
            poolSlot.pool.occupiedPoolSlots.set(poolSlot.slot, false);
        }
    }

    public void moveCustomerToExit(Circle customer, Runnable onFinished) {
        Point2D exitPosition = AnimationHelper.centerOf(exitZone);
        moveWithShortDelay(customer, exitPosition, () -> {
            releasePendingPoolSlot(customer);
            animationPane.getChildren().remove(customer);
            if (onFinished != null) {
                onFinished.run();
            }
        });
    }

    private void moveWithShortDelay(Circle customer, Point2D target, Runnable onFinished) {
        Timeline timeline = new Timeline();
        int delay = ThreadLocalRandom.current().nextInt(200, 500);

        KeyFrame keyFrame = new KeyFrame(
                Duration.millis(delay),
                event -> AnimationHelper.moveTo(customer, target, onFinished)
        );

        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    private void releaseQueueSlot(Circle customer, PoolViewState pool) {
        Integer queueSlot = customerQueueSlots.remove(customer);
        if (queueSlot != null) {
            pool.occupiedQueueSlots.set(queueSlot, false);
        }
    }

    private Point2D getPosition(PoolViewState pool, int slot, boolean isPool) {
        double startX;
        double startY;
        int columns;

        if(isPool) {
            startX = pool.poolZone.getLayoutX() + PADDING;
            startY = pool.poolZone.getLayoutY() + PADDING;
            columns = (int) ((pool.poolZone.getWidth() - 2 * PADDING) / SPACING);
        }else {
            startX = pool.waitZone.getLayoutX() + PADDING;
            startY = pool.waitZone.getLayoutY() + PADDING;
            columns = (int) ((pool.waitZone.getWidth() - 2 * PADDING) / SPACING);
        }

        int row = 0;
        int column = 0;

        for (int i = 0; i < slot; i++) {
            column++;

            if (column >= columns) {
                row++;
                column = 0;
            }
        }

        double x = startX + column * SPACING;
        double y = startY + row * SPACING;

        return new Point2D(x, y);
    }

    private int reserveSlot(PoolViewState pool, boolean isPool) {
        if(isPool) {
            for (int i = 0; i < pool.occupiedPoolSlots.size(); i++) {
                if (!pool.occupiedPoolSlots.get(i)) {
                    pool.occupiedPoolSlots.set(i, true);
                    return i;
                }
            }
            pool.occupiedPoolSlots.add(true);
            return pool.occupiedPoolSlots.size() - 1;
        }else{
            for (int i = 0; i < pool.occupiedQueueSlots.size(); i++) {
                if (!pool.occupiedQueueSlots.get(i)) {
                    pool.occupiedQueueSlots.set(i, true);
                    return i;
                }
            }
            pool.occupiedQueueSlots.add(true);
            return pool.occupiedQueueSlots.size() - 1;
        }
    }

    private PoolViewState findPoolState(String poolName) {
        PoolViewState pool = pools.get(poolName);
        if (pool == null) {
            throw new IllegalArgumentException("Nieznany basen: " + poolName);
        }
        return pool;
    }

    private static int getQueueSlotCount(Rectangle waitZone) {
        int columns = (int) ((waitZone.getWidth() - 2 * PADDING) / SPACING);
        int rows = (int) ((waitZone.getHeight() - 2 * PADDING) / SPACING);

        return columns * rows;
    }

    private static class PoolViewState {
        private final Rectangle poolZone;
        private final Rectangle waitZone;
        private final List<Boolean> occupiedPoolSlots = new ArrayList<>();
        private final List<Boolean> occupiedQueueSlots = new ArrayList<>();

        private PoolViewState(Rectangle poolZone, Rectangle waitZone, int poolCapacity) {
            this.poolZone = poolZone;
            this.waitZone = waitZone;
            for (int i = 0; i < poolCapacity; i++) {
                this.occupiedPoolSlots.add(false);
            }
            for (int i = 0; i < getQueueSlotCount(waitZone); i++) {
                this.occupiedQueueSlots.add(false);
            }
        }
    }

    private static class PoolSlot {
        private final PoolViewState pool;
        private final int slot;

        private PoolSlot(PoolViewState pool, int slot) {
            this.pool = pool;
            this.slot = slot;
        }
    }
}
