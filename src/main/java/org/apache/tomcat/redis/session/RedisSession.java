package org.apache.tomcat.redis.session;

import org.apache.catalina.session.StandardSession;

import java.io.Serializable;
import java.security.Principal;

/**
 * RedisSession which has the required hook to register <code>RedisCommand</code>s whenever any activity is happening on the <code>Session</code>.
 * It is not required for the <code>Session</code> object to be manipulated only by the web request thread.
 */
class RedisSession extends StandardSession {

  protected RedisSessionActionHandler actionHandler = null;

  /**
   * Construct a new Session associated with the specified Manager.
   *
   * @param manager The manager with which this Session is associated
   */
  public RedisSession(RedisSessionManager manager) {
    super(manager);

    this.actionHandler = manager.getActionHandler();
  }

  @Override
  public void recycle() {
    super.recycle();

    this.actionHandler.removeSession(this);
  }

  @Override
  public void setPrincipal(Principal principal) {
    assertSerializable(principal);

    super.setPrincipal(principal);

    this.actionHandler.registerSessionPrincipal(this);
  }

  @Override
  public void setCreationTime(long time) {
    super.setCreationTime(time);

    this.actionHandler.registerSessionCreationTime(this);
  }

  @Override
  public void setAuthType(String authType) {
    super.setAuthType(authType);

    this.actionHandler.registerSessionAuthType(this);
  }

  @Override
  public void removeNote(String name) {
    super.removeNote(name);

    this.actionHandler.removeSessionNote(this, name);
  }

  @Override
  public void setNote(String name, Object value) {
    assertSerializable(value);

    super.setNote(name, value);

    this.actionHandler.registerSessionNote(this, name, value);
  }

  @Override
  public void removeAttribute(String name, boolean notify) {
    super.removeAttribute(name, notify);

    this.actionHandler.removeSessionAttribute(this, name);
  }

  @Override
  public void setAttribute(String name, Object value, boolean notify) {
    assertSerializable(value);

    super.setAttribute(name, value, notify);

    this.actionHandler.registerSessionAttribute(this, name, value);
  }

  @Override
  public String toString() {
    return "RedisSession[" + id + "]";
  }

  /**
   * It is mandatory that all the values are <code>Serializable</code>. This method ensures this.
   * @param obj <code>Object</code> that is being asserted as <code>Serializable</code>.
   */
  private void assertSerializable(Object obj) {
    if (obj != null && !(obj instanceof Serializable)) {
      throw new IllegalArgumentException("Value registered in session has to be serializable and so should implement java.io.Serialiable.");
    }
  }

}
