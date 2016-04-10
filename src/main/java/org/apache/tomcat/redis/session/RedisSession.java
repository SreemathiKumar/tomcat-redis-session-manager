package org.apache.tomcat.redis.session;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;


class RedisSession extends StandardSession {

  /**
   * Construct a new Session associated with the specified Manager.
   *
   * @param manager The manager with which this Session is associated
   */
  public RedisSession(Manager manager) {
    super(manager);
  }

}
