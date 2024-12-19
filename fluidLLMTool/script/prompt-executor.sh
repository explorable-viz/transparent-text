#!/bin/bash

# Function to install Java
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
    echo "Usage: $0 <agent_class> <prompt_configuration> <sentences> [expected_results] [threshold]"
    echo "  agent_class                   (mandatory): LLMAgent to execute"
    echo "  prompt_configuration          (mandatory): prompt configuration path  (format: JSON)"
    echo "  sentences                     (mandatory): input file"
    echo "  expected_results              (optional) : expected output file (for validation only)"
    exit 1
fi

# Assign the first two mandatory parameters
agent_class=$1
prompt_configuration=$2
sentences=$3

# The expected_results parameter is optional
expected_results=${4:-}

# The threshold parameter is optional
threshold=${5:-}
# If the threshold is not set, the default value is 0.7 (70%)

if [ -z "$threshold" ]; then
    threshold=0.7
fi

# Run the Java command with the parameters
if [ -z "$expected_results" ]; then
    output=$(java --enable-preview -jar target/fluidPrompt-0.1-jar-with-dependencies.jar "$agent_class" "$prompt_configuration" "$sentences")
else
    output=$(java --enable-preview -jar target/fluidPrompt-0.1-jar-with-dependencies.jar "$agent_class" "$prompt_configuration" "$sentences" "$expected_results")
fi

# Analyses the output with a regex to extract the accuracy
if [[ $output =~ Accuracy:\ ([0-9]*\.[0-9]+) ]]; then
    accuracy=${BASH_REMATCH[1]}
    # Controlla se l'accuracy è inferiore a 0.7
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
