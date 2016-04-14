Redis Session Manager for Apache Tomcat
=======================================

Overview
--------

An session manager implementation that stores sessions in Redis for easy distribution of requests across a cluster of Tomcat servers. Sessions are implemented as as non-sticky--that is, each request is able to go to any server in the cluster (unlike the Apache provided Tomcat clustering setup.). To save the load on infrastructure and redis, the changes are persisted at attribute level. Calls to remove or set any of the following properties will be recorded for persistence.

- principal
- authType
- creationTime
- id
- notes
- attributes

Sessions are stored into Redis asynchronously upon creation for use by other servers. Sessions are loaded as requested directly from Redis. In order to prevent collisions (and lost writes) as much as possible, session data is updated in Redis asynchronously as when the session has been modified.

The manager relies on the native expiration capability of Redis to expire keys for automatic session expiration to avoid the overhead of constantly searching the entire list of sessions for expired sessions.

Data stored in the session must be Serializable.

Tomcat Versions
---------------

This project supports Tomcat 7. Starting at project version 7.0.1. The versioning nomenclature is as follows

x.y.z where x.y represents the major version of the tomcat. Example, 7.0.1 supports tomcat 7.0. 

The official release branches in Git are as follows:
* `master`: Continuing work for Tomcat 7 releases. Compatible with Java 7.

Architecture
------------

* RedisSessionHandlerValve: ensures that sessions changes are saved asynchronously into redis.
* RedisSessionManager: provides the session creation, saving, and loading functionality.
* RedisSession : provides hooks to register changes done on a session.
* RedisSessionActionHandler : provides capabilities to register the changes done on a session and creates actions to be executed in redis.
* RedisStoreManager : provides capabilities to run the changes on a session in an asynchronous manner.

Note: This architecture differs from the Apache PersistentManager implementation which implements persistent sticky sessions. Because that implementation expects all requests from a specific session to be routed to the same server, the timing persistence of sessions is non-deterministic since it is primarily for failover capabilities.

Usage
-----

Add the following into your Tomcat context.xml (or the context block of the server.xml if applicable.)

    <Valve className="org.vijaysanthosh.tomcat.redis.session.RedisSessionHandlerValve" />
    <Manager className="org.vijaysanthosh.tomcat.redis.session.RedisSessionManager"
             host="localhost" <!-- optional: defaults to "localhost" -->
             port="6379" <!-- optional: defaults to "6379" -->
             database="0" <!-- optional: defaults to "0" -->
             executorPoolSize="10" <!-- optional: defaults to number of processors -->
             maxInactiveInterval="60" <!-- optional: defaults to "60" (in seconds) -->
             sentinelMaster="SentinelMasterName" <!-- optional -->
             sentinels="sentinel-host-1:port,sentinel-host-2:port,.." <!-- optional --> />

The Valve must be declared before the Manager.

Copy the following files into the `TOMCAT_BASE/lib` directory:

* session-manager-VERSION.jar
* [jedis-2.8.1.jar](http://central.maven.org/maven2/redis/clients/jedis/2.8.1/jedis-2.8.1.jar)
* [commons-pool2-2.4.2.jar](http://central.maven.org/maven2/org/apache/commons/commons-pool2/2.4.2/commons-pool2-2.4.2.jar)

Reboot the server, and sessions should now be stored in Redis.

Connection Pool Configuration
-----------------------------

All of the configuration options from both `org.apache.commons.pool2.impl.GenericObjectPoolConfig` and `org.apache.commons.pool2.impl.BaseObjectPoolConfig` are also configurable for the Redis connection pool used by the session manager. To configure any of these attributes (e.g., `maxIdle` and `testOnBorrow`) just use the config attribute name prefixed with `connectionPool` (e.g., `connectionPoolMaxIdle` and `connectionPoolTestOnBorrow`) and set the desired value in the `<Manager>` declaration in your Tomcat context.xml.

Session Change Tracking
-----------------------

As noted in the 'Overview' section above, the Redis Session Manager serializes the session object into Redis if the session object has changed. The changes done to a session are registered and persisted asynchronously. This has no impact on the performance of the website with heavy objects are to be persisted into redis.

This feature can have the unintended consequence of hiding writes if you implicitly change a key in the session. For example, assuming the session already contains the key `"myArray"` with an Array instance as its corresponding value, and has been previously serialized, the following code would not cause the session to be serialized again:

    List myArray = session.getAttribute("myArray");
    myArray.add(additionalArrayValue);

If your code makes these kind of changes, then the RedisSession provides a mechanism by which you can mark the changes done to the attribute by setting the same instance into the same key of the session. For example:

    List myArray = session.getAttribute("myArray");
    myArray.add(additionalArrayValue);
    session.setAttribute("myArray", myArray); // This registers the changes done to the attribute "myArray"

Acknowledgements
----------------

The documentation of this project is based on the [Redis Session Manager for Apache Tomcat](https://github.com/jcoleman/tomcat-redis-session-manager) project.
