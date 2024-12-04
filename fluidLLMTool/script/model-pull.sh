#!/bin/bash
# Check that the command is executed with the right parameters
if [ $# -lt 1 ]; then
    echo "Usage: $0 <model_name:version>"
    echo "  param1          (mandatory): model name (like llama3, llama3.1. Check on https://github.com/ollama/ollama the available models)"
    exit 1
fi

ollama pull $1
