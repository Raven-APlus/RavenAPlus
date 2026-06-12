package keystrokesmod.utility;

import keystrokesmod.Raven;
import keystrokesmod.event.PostUpdateEvent;
import keystrokesmod.utility.PacketUtils;
import net.minecraft.network.Packet;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Queues outbound packets that must not be sent on the same tick as conflicting packets (C07/C02/C08).
 */
public class PacketScheduler {
    public enum Priority {
        LOW(0),
        NORMAL(1),
        HIGH(2);

        private final int value;

        Priority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private static final List<QueuedPacket> queue = new ArrayList<>();

    public static boolean canSendAttack() {
        return Raven.badPacketsHandler.canSendAttack();
    }

    public static boolean canSendBlockPlacement() {
        return Raven.badPacketsHandler.canSendBlock();
    }

    public static void queue(Packet<?> packet, Priority priority) {
        if (packet == null) {
            return;
        }
        synchronized (queue) {
            queue.add(new QueuedPacket(packet, priority));
        }
    }

    public static void flush() {
        synchronized (queue) {
            if (queue.isEmpty()) {
                return;
            }
            queue.sort(Comparator.comparingInt(q -> -q.priority.getValue()));
            for (QueuedPacket queued : new ArrayList<>(queue)) {
                if (queued.packet instanceof net.minecraft.network.play.client.C02PacketUseEntity
                        && !canSendAttack()) {
                    continue;
                }
                if (queued.packet instanceof net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
                        && !canSendBlockPlacement()) {
                    continue;
                }
                PacketUtils.sendPacket(queued.packet);
                queue.remove(queued);
            }
        }
    }

    public static void clear() {
        synchronized (queue) {
            queue.clear();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPostUpdate(PostUpdateEvent event) {
        flush();
    }

    private static final class QueuedPacket {
        private final Packet<?> packet;
        private final Priority priority;

        private QueuedPacket(Packet<?> packet, Priority priority) {
            this.packet = packet;
            this.priority = priority;
        }
    }
}
