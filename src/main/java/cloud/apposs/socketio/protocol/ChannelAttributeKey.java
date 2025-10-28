package cloud.apposs.socketio.protocol;

import io.netty.util.AttributeKey;

public final class ChannelAttributeKey {
    public static final AttributeKey<String> ORIGIN = AttributeKey.valueOf("origin");
    public static final AttributeKey<String> USER_AGENT = AttributeKey.valueOf("userAgent");
    public static final AttributeKey<String> NAMESPACE = AttributeKey.valueOf("namespace");
    public static final AttributeKey<Boolean> B64 = AttributeKey.valueOf("b64");
    public static final AttributeKey<Integer> JSONP_INDEX = AttributeKey.valueOf("jsonpIndex");
    public static final AttributeKey<Boolean> WRITE_ONCE = AttributeKey.valueOf("writeOnce");
}
