package cloud.apposs.socketio.ack;

import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.protocol.Packet;
import cloud.apposs.socketio.protocol.PacketType;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AckRequest {
    private final Packet packet;

    private final SocketIOSession session;

    private final AtomicBoolean sended = new AtomicBoolean();

    public AckRequest(SocketIOSession session, Packet packet) {
        this.session = session;
        this.packet = packet;
    }

    /**
     * Check whether ack request was made
     *
     * @return true if ack requested by client
     */
    public boolean isAckRequested() {
        return packet.isAckRequested();
    }

    public void sendAckData(Object ... objs) {
        List<Object> args = Arrays.asList(objs);
        sendAckData(args);
    }

    public void sendAckData(List<Object> objs) {
        if (!isAckRequested() || !sended.compareAndSet(false, true)) {
            return;
        }
        Packet ackPacket = new Packet(PacketType.MESSAGE, session.getVersion());
        ackPacket.setSubType(PacketType.ACK);
        ackPacket.setAckId(packet.getAckId());
        ackPacket.setData(objs);
        session.send(ackPacket);
    }
}
