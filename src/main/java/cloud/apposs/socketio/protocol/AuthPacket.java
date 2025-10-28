package cloud.apposs.socketio.protocol;

import java.util.UUID;

public class AuthPacket {
    private final UUID sid;
    private final String[] upgrades;
    private final int pingInterval;
    private final int pingTimeout;

    public AuthPacket(UUID sid, String[] upgrades, int pingInterval, int pingTimeout) {
        super();
        this.sid = sid;
        this.upgrades = upgrades;
        this.pingInterval = pingInterval;
        this.pingTimeout = pingTimeout;
    }

    public int getPingInterval() {
        return pingInterval;
    }

    public int getPingTimeout() {
        return pingTimeout;
    }

    public UUID getSid() {
        return sid;
    }

    public String[] getUpgrades() {
        return upgrades;
    }
}
