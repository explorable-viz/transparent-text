#!/bin/bash
set -xe

# Function to install Java [@todo move to another script]
install_java() {
    echo "Java is not installed. Installation attempt..."
    if command -v apt &> /dev/null; then
        # Debian/Ubuntu based
        sudo apt update
        sudo apt install -y default-jre
    elif command -v yum &> /dev/null; then
        # CentOS/RHEL based
        sudo yum install -y java-11-openjdk
    elif command -v brew &> /dev/null; then
        # macOS based
        brew update
        brew install openjdk
        # Symbolic link for macos.
        sudo ln -sfn "$(brew --prefix openjdk)/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk.jdk
    else
        echo "Error: neither apt nor yum found. Java installation failed."
        exit 1
    fi

    # Verifica se l'installazione ha avuto successo
    if ! command -v java &> /dev/null; then
        echo "Error: Java installation failed."
        exit 1
    fi
    echo "Java successfully installed."
}

# Check if Java is installed
if ! command -v java &> /dev/null; then
    install_java
fi

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
# [@todo comment]
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

base_command="java --enable-preview -jar target/PromptExecutor-0.1-jar-with-dependencies.jar agent=$agent_class"

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
