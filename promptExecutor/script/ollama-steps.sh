#!/bin/bash
set -xe

./script/ollama-install.sh
./script/ollama-model-pull.sh llama3.1
./script/ollama-serve.sh
