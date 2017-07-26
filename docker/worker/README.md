# xtremweb-hep
Data driven volunteer cloud

### Status
[![Build Status](https://travis-ci.org/lodygens/xtremweb-hep.svg?branch=master)](https://travis-ci.org/lodygens/xtremweb-hep)

Docker deployment
=================

This directory contains everything to start a worker inside a container from its Debian package.

The worker Debian package can be generated from [docker/server-master](../server-master).

## Deployment

To start a worker, launch startworker.sh like:
```
  startworker.sh
```

This script :
- builds a new Docker image for the XWHEP worker;
- starts the XWHEP worker in a new Docker container.

