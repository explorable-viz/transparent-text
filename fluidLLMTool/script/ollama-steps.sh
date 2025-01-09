#!/bin/bash
set -xe

./script/ollama-install.sh
./script/model-pull.sh llama3.1
./script/ollama-serve.sh
