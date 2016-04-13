package org.apache.tomcat.redis.session;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * This <code>ValveBase</code> implementation ensures that the request processing is taken care of in the redis for any session access.
 */
public class RedisSessionHandlerValve extends ValveBase {
    private RedisSessionManager manager;

    /**
     * Sets <code>RedisSessionManager</code>
     * @param manager The <code>RedisSessionManager</code> to be set.
     */
    public void setRedisSessionManager(RedisSessionManager manager) {
        this.manager = manager;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        this.manager.preRequestProcessing(request);
        try {
            getNext().invoke(request, response);
        } finally {
            this.manager.postRequestProcessing(request);
        }
    }
}
