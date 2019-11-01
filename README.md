# Reactive Microservices Sample

- Services communicate directly over TCP (or websocket) using RSocket.  
- Services can also accept any assigned port.  The port and hostname are reported to the Eureka server and distributed to ``@DiscoveryClient`` instances.  
- Gateway service is also a client of Eureka and can automatically locate and load balance (Ribbon) requests.

## To Run
- Start the ``eureka-server``
- Start the client services ``account-service`` and ``profile-service``
- Start the ``gateway-service``

Order is theoretically not important, but it takes time for the services to discover each other.  If you're impatient or frequently restarting a service, you may find the registrations to get outdated and return incorrect responses or statuses.  Safest bet is to restart all of the them.  These are small services; it's fast. 

## Links
- URIs starting with ``/account`` are directed to the ``account-service``.
- URIs starting with ``/profile`` are directed to the ``profile-service``.
- Services are available at their own address, but you have to find the server port in the log for that service.
- All requests can be sent through the gateway at ``localhost:8080``


| URI | Service | Description |
| --- | --- | --- |
| ``/account/all`` | ``account-service`` | Retrieve all accounts in the database created on startup |
| ``/account/sse/<name>`` | ``account-service`` | Open a stream of produced data using the input name |
| ``/profile/sse/<name>`` | ``profile-service`` | Communicate over TCP with ``account-service`` to get the same stream as above, but proxied between the services |
