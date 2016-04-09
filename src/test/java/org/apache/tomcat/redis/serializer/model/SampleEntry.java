package org.apache.tomcat.redis.serializer.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement(name="Entry")
public class SampleEntry implements Serializable {
    private int statusCode;
    private String msg;
    private SimpleEntry entry;

    @XmlElement(name="Msg")
    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @XmlElement(name="StatusCode")
    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public SimpleEntry getEntry() {
        return entry;
    }

    public void setEntry(SimpleEntry entry) {
        this.entry = entry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SampleEntry that = (SampleEntry) o;

        return statusCode == that.statusCode &&
                !(msg != null ? !msg.equals(that.msg) : that.msg != null) &&
                !(entry != null ? !entry.equals(that.entry) : that.entry != null);

    }

    @Override
    public int hashCode() {
        int result = statusCode;
        result = 31 * result + (msg != null ? msg.hashCode() : 0);
        result = 31 * result + (entry != null ? entry.hashCode() : 0);
        return result;
    }
}