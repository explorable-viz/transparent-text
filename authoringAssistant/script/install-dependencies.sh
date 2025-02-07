#!/bin/bash
set -xe

install_dependencies() {

    if [[ $OSTYPE == 'darwin'* ]]; then
        JAVA_URL="https://download.java.net/java/GA/jdk22.0.2/c9ecb94cd31b495da20a27d4581645e8/9/GPL/openjdk-22.0.2_macos-aarch64_bin.tar.gz"
        JAVA_TAR="openjdk-22.0.2_macos-aarch64_bin.tar.gz"
        INSTALL_DIR="/Library/Java/JavaVirtualMachines"

        # Download OpenJDK
        curl -L -o "$JAVA_TAR" "$JAVA_URL"

        # Extract and move to system location
        sudo mkdir -p "$INSTALL_DIR"
        sudo tar -xzf "$JAVA_TAR" -C "$INSTALL_DIR"
        sudo mv "$INSTALL_DIR"/jdk-22.0.2.jdk "$INSTALL_DIR"/openjdk.jdk

        # Cleanup tar file
        rm "$JAVA_TAR"

        # Ensure JAVA_HOME is set correctly
        export JAVA_HOME="$INSTALL_DIR/openjdk.jdk/Contents/Home"
        echo "export JAVA_HOME=$JAVA_HOME" | sudo tee -a /etc/profile
        echo "export PATH=\$JAVA_HOME/bin:\$PATH" | sudo tee -a /etc/profile
        source /etc/profile

        brew update
        brew install maven
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

install_dependencies
