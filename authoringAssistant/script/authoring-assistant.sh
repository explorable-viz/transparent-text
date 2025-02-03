#!/bin/bash
set -xe

# Check that at least the first two parameters are provided
if [ $# -lt 3 ]; then
    echo "Usage: $0 <agent_class> <prompt_configuration> <testCases> [expected_results] [threshold]"
    echo "  agent_class                   (mandatory): LLMAgent to execute"
    echo "  prompt_configuration          (mandatory): prompt configuration path  (format: JSON)"
    echo "  settings path                 (mandatory): settings configuration path  (format: JSON)"
    echo "  testCases                       (mandatory): input file"
    echo "  expected_results              (optional) : expected output file (for validation only)"
    echo "  threshold                     (optional) : the minimum accuracy to consider successfully the test execution"
    echo "  max_testCases                   (optional) : number of testCases to test during the execution"
    echo "  templatePath                  (optional) : fluid templatePath"
    exit 1
fi

agent_class=$1
prompt_configuration=$2
testCases=$4
settings=$3
threshold=${5:-}
max_testCases=${6:-}
numTestToGenerate=${7:-}

if [ -z "$threshold" ]; then
    threshold=0.7
fi

base_command="java --enable-preview -jar target/authoringAssistant-0.1-jar-with-dependencies.jar agent=$agent_class"

command="$base_command inContextLearningPath=$prompt_configuration settingsPath=$settings testPath=$testCases"

if [ -n "$numTestToGenerate" ]; then
    command="$command numQueryToExecute=$max_testCases numTestToGenerate=$numTestToGenerate"
fi

output=$(eval "$command")

# Analyses the output with a regex to extract the accuracy
if [[ $output =~ Accuracy:\ ([0-9]*\.[0-9]+) ]]; then
    accuracy=${BASH_REMATCH[1]}
    # check if the accuracy is less than the $threshold
    if (( $(echo "$accuracy < $threshold" | bc -l) )); then
        echo "FAILED: Accuracy too low ($accuracy < $threshold)"
        exit 1
    else
        echo "PASS: Accuracy is sufficient ($accuracy > $threshold)"
        exit 0
    fi
else
    echo "ERROR: Could not parse accuracy from output"
    exit 1
fi
