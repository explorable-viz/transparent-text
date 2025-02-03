#!/bin/bash
set -xe

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
    echo "pippo"
fi
