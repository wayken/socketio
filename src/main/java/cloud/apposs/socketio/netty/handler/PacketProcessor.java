package cloud.apposs.socketio.netty.handler;

import cloud.apposs.socketio.SocketIOContextHolder;
import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.ack.AckManager;
import cloud.apposs.socketio.ack.AckRequest;
import cloud.apposs.socketio.protocol.EngineIOVersion;
import cloud.apposs.socketio.protocol.Packet;
import cloud.apposs.socketio.protocol.PacketType;
import cloud.apposs.socketio.scheduler.CancelableScheduler;
import cloud.apposs.socketio.scheduler.SchedulerKey;
import cloud.apposs.socketio.transport.Transport;

import java.util.Collections;
import java.util.List;

public class PacketProcessor {
    private final SocketIOContextHolder context;

    private final CancelableScheduler scheduler;

    private final AckManager ackManager;

    public PacketProcessor(SocketIOContextHolder context, CancelableScheduler scheduler, AckManager ackManager) {
        this.context = context;
        this.scheduler = scheduler;
        this.ackManager = ackManager;
    }

    public void onPacket(SocketIOSession session, Packet packet, Transport transport) throws Exception {
        final AckRequest ackRequest = new AckRequest(session, packet);
        if (packet.isAckRequested()) {
            ackManager.initAckIndex(session.getSessionId(), packet.getAckId());
        }
        switch (packet.getType()) {
            case PING: {
                Packet outPacket = new Packet(PacketType.PONG, session.getVersion());
                outPacket.setData(packet.getData());
                session.send(outPacket, transport);
                if ("probe".equals(packet.getData())) {
                    session.send(new Packet(PacketType.NOOP, session.getVersion()), Transport.POLLING);
                } else {
                    session.schedulePingTimeout();
                }
                break;
            }

            case PONG: {
                session.schedulePingTimeout();
                break;
            }

            case UPGRADE: {
                session.schedulePingTimeout();
                SchedulerKey key = new SchedulerKey(SchedulerKey.Type.UPGRADE_TIMEOUT, session.getSessionId());
                scheduler.cancel(key);
                session.handleTransportUpgrade(transport);
                break;
            }

            case MESSAGE: {
                session.schedulePingTimeout();
                if (packet.getSubType() == PacketType.DISCONNECT) {
                    session.onChannelDisconnect();
                }
                if (packet.getSubType() == PacketType.CONNECT) {
                    // send connect handshake packet back to client
                    if (!EngineIOVersion.V4.equals(session.getVersion())) {
                        session.send(packet, transport);
                    }
                }
                if (packet.getSubType() == PacketType.ACK || packet.getSubType() == PacketType.BINARY_ACK) {
                    ackManager.onAck(session, packet);
                }
                if (packet.getSubType() == PacketType.EVENT || packet.getSubType() == PacketType.BINARY_EVENT) {
                    List<Object> args = Collections.emptyList();
                    if (packet.getData() != null) {
                        args = packet.getData();
                    }
                    context.onEvent(session, packet, args);
                }
                break;
            }

            case CLOSE:
                session.onChannelDisconnect();
                break;

            default:
                break;
        }
    }
}
