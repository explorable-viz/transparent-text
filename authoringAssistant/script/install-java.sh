#!/bin/bash
set -xe

# Function to install Java
install_java() {
    echo "Java is not installed. Installation attempt..."
    if [[ $OSTYPE == 'darwin'* ]]; then
        brew update
        brew install openjdk maven
        # Symbolic link for macos.
        sudo ln -sfn "$(brew --prefix openjdk)/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk.jdk
    elif command -v apt &> /dev/null; then
        # Debian/Ubuntu based
        sudo apt update
        sudo apt install -y default-jre default-jdk maven
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
    java --version
}

install_java
