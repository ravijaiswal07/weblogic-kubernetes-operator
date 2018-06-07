#!/bin/bash

set -x

# TBD - need to delete the k8s resources before deleting the actual storage
# but delete-domain-resources.sh doesn't return an error if the resources
# never finish deleting

# For now, run these 2 scripts individually and don't run the second until
# the k8s resources for the domain actually gone away

delete-domain-resources.sh
delete-domain-pvs.sh
