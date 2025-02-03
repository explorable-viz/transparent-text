#!/bin/bash
set -xe

# Check that the command is executed with the right parameters
if [ $# -lt 1 ]; then
    echo "Usage: $0 <model_name:number_parameters>"
    echo "model_name:number_parameters         (mandatory): model name (like llama3, llama3.1. Check on https://github.com/ollama/ollama the available models)"
    exit 1
fi

ollama pull $1

if [ $? -ne 0 ]; then
    echo "Error: 'ollama pull $1' failed!" >&2
    exit 1
fi
