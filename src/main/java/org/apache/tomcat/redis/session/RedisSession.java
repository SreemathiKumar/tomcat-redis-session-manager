package org.apache.tomcat.redis.session;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;

import java.security.Principal;


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
    super.setPrincipal(principal);

    this.actionHandler.setSessionPrincipal(this, principal);
  }

  @Override
  public void setCreationTime(long time) {
    super.setCreationTime(time);

    this.actionHandler.setSessionCreationTime(this, time);
  }

  @Override
  public void setAuthType(String authType) {
    super.setAuthType(authType);

    this.actionHandler.setSessionAuthType(this, authType);
  }

  @Override
  public void removeNote(String name) {
    super.removeNote(name);

    this.actionHandler.removeSessionNote(this, name);
  }

  @Override
  public void setNote(String name, Object value) {
    super.setNote(name, value);

    this.actionHandler.setSessionNote(this, name, value);
  }

  @Override
  public void removeAttribute(String name, boolean notify) {
    super.removeAttribute(name, notify);

    this.actionHandler.removeSessionAttribute(this, name);
  }

  @Override
  public void setAttribute(String name, Object value, boolean notify) {
    super.setAttribute(name, value, notify);

    this.actionHandler.setSessionAttribute(this, name, value);
  }

  @Override
  public String toString() {
    return "RedisSession[" + id + "]";
  }

}
