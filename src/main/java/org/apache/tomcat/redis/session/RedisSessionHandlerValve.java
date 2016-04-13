package org.apache.tomcat.redis.session;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import javax.servlet.ServletException;
import java.io.IOException;


public class RedisSessionHandlerValve extends ValveBase {
    private RedisSessionManager manager;

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
