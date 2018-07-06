#!/bin/bash

set -x

export DEMO_HOME=`pwd` # this means that you need to run the sample from this directory!
export OPERATOR_HOME="${DEMO_HOME}/../kit"
export OPERATOR_SAMPLES="${OPERATOR_HOME}/samples"

#----------------------------------------------------------------------------------------
# All/most customers need these functions as-is
#----------------------------------------------------------------------------------------


#----------------------------------------------------------------------------------------
# Functionality specific to this domain
#----------------------------------------------------------------------------------------


function createDockerImage {
  docker build --no-cache -t wls-12213-domain-in-image .
}

function main {
  cp ${OPERATOR_SAMPLES}/create-domain-home-with-configured-cluster.sh .
  createDockerImage
}

main
