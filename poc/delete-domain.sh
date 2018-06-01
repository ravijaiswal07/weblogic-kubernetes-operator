#!/bin/bash

set -x

# TBD - need to delete the k8s resources before deleting the actual storage
# but delete-domain-resources.sh doesn't wait for the resources to be deleted.

# For now, run these 2 scripts individually and don't run the second until
# the k8s resources for the domain actually gone away

#delete-domain-resources.sh
#delete-domain-pvs.sh
