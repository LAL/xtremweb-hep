# xtremweb-hep
Data driven volunteer cloud

### Status
[![Build Status](https://travis-ci.org/lodygens/xtremweb-hep.svg?branch=master)](https://travis-ci.org/lodygens/xtremweb-hep)

Docker deployment
=================

This directory contains everything to start your own deployment from last sources.

## Security
If you wish to deploy a production platform, don't forget to set your own configation in xwconfigure.values file.
Please refer to the [administrator guide](../../doc/xwhep-admin-guide.odt).

## Deployment

Launch xwdeploy.sh like:
```
  xwdeploy.sh
```

This script :
- builds a new Docker image for the XWHEP server;
- starts a new Docker container for the XWHEP server;
- copy XWHEP worker and client Debian packages from the running container to host.

These Debian packages can avantageoulsy be used with [docker/worker](../worker) and [docker/client](../client) to
create and launch Docker containers that connect to the XWHEP server inside its container.
