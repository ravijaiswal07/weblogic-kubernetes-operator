#!/bin/bash

export DEMO_HOME=`pwd` # this means that you need to run the sample from this directory!
export SETUP_SCRIPT_ENV_SCRIPT="${DEMO_HOME}/domain-env.sh"

export OPERATOR_HOME="${DEMO_HOME}/../kit"
export OPERATOR_SAMPLES="${OPERATOR_HOME}/samples"
export OPERATOR_TEMPLATES="${OPERATOR_HOME}/templates"
export GENERATED_FILES="${DEMO_HOME}/generated"
export PVS_DIR="/scratch/k8s-dir"

export DEMO_NAME="demo1"

export OPERATOR_NAMESPACE="demo-o-ns"
export DOMAINS_NAMESPACE="demo-d-ns"
