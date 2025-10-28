package cloud.apposs.socketio.protocol;

import io.netty.handler.codec.http.HttpHeaders;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class HandshakeData implements Serializable {
    private static final long serialVersionUID = 1296350300161819978L;

    private HttpHeaders headers;
    private InetSocketAddress address;
    private Date time = new Date();
    private InetSocketAddress local;
    private String url;
    private Map<String, List<String>> urlParams;
    private boolean xdomain;

    // needed for correct deserialization
    public HandshakeData() {
    }

    public HandshakeData(HttpHeaders headers, Map<String, List<String>> urlParams, InetSocketAddress address, String url, boolean xdomain) {
        this(headers, urlParams, address, null, url, xdomain);
    }

    public HandshakeData(HttpHeaders headers, Map<String, List<String>> urlParams, InetSocketAddress address, InetSocketAddress local, String url, boolean xdomain) {
        super();
        this.headers = headers;
        this.urlParams = urlParams;
        this.address = address;
        this.local = local;
        this.url = url;
        this.xdomain = xdomain;
    }

    /**
     * Client network address
     *
     * @return network address
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * Connection local address
     *
     * @return local address
     */
    public InetSocketAddress getLocal() {
        return local;
    }

    /**
     * Http headers sent during first client request
     *
     * @return headers
     */
    public HttpHeaders getHttpHeaders() {
        return headers;
    }

    /**
     * Client connection date
     *
     * @return date
     */
    public Date getTime() {
        return time;
    }

    /**
     * Url used by client during first request
     *
     * @return url
     */
    public String getUrl() {
        return url;
    }

    public boolean isXdomain() {
        return xdomain;
    }

    /**
     * Url params stored in url used by client during first request
     *
     * @return map
     */
    public Map<String, List<String>> getUrlParams() {
        return urlParams;
    }

    public String getSingleUrlParam(String name) {
        List<String> values = urlParams.get(name);
        if (values != null && values.size() == 1) {
            return values.iterator().next();
        }
        return null;
    }
}
