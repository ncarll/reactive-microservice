# Reactive Microservices Sample

- Services communicate directly over TCP or websocket using RSocket.  
- Services can also accept any assigned port.  The port and hostname are reported to the Eureka server and distributed to ``@DiscoveryClient``.  
- Gateway service is also a client of Eureka and can automatically locate and load balance (Ribbon) requests.

## To Run
- Start the ``eureka-server``
- Start the client services ``account-service`` and ``profile-service``
- Start the ``gateway-service``

Order is theoretically not important, but it takes time for the services to discover each other.  If you're impatient or frequently restarting a server, you may find the registrations to get outdated and return incorrect responses or statuses.  Safest bet is to restart all of the services.  These are small services; it's fast. 