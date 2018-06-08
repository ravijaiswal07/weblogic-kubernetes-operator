#!/bin/bash

set -x

# delete-domain.sh ?

create-domain-home.sh
patch-domain-home.sh
create-domain-pvs.sh
op-bind-domain.sh
create-domain-resources.sh
