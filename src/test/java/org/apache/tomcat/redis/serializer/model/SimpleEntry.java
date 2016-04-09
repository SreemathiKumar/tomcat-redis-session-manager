package org.apache.tomcat.redis.serializer.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

public class SimpleEntry implements Serializable {
    private int statusCode;
    private String msg;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleEntry that = (SimpleEntry) o;

        return statusCode == that.statusCode && !(msg != null ? !msg.equals(that.msg) : that.msg != null);

    }

    @Override
    public int hashCode() {
        int result = statusCode;
        result = 31 * result + (msg != null ? msg.hashCode() : 0);
        return result;
    }
}