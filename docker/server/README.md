Docker XWHEP server
===================

This directory contains everything to start an XWHEP server inside a container from its Debian package.

The server Debian package can be generated from [docker/server-master](../server-master).

# Deployment

To start a server, launch startserver.sh like:
```
  startserver.sh
```

This script :
- builds a new Docker image for the XWHEP server;
- starts the XWHEP server in a new Docker container.

