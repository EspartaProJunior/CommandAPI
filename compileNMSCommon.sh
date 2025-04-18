#!/bin/bash

# Make sure we are running at the script's location (CommandAPI root)
cd "${0%/*}" || (echo "cd failed" && exit)

BUILD_DIR="./commandapi-platforms/commandapi-bukkit/commandapi-bukkit-nms/commandapi-bukkit-nms-common/target"
DIFF_DIR="./nmsCommonDiff"
NEW_JAR="./newJar.jar"
NEW_CLASSES="./newClasses"
PREV_CLASSES="./prevClasses"

# Refresh diff directory
rm -rf "$DIFF_DIR"
mkdir -p "$DIFF_DIR"

compileVersion() {
    echo "Compiling NMS_Common for Spigot $1..."

    mvn clean package -Dmaven.source.skip=true -Dmaven.javadoc.skip=true \
    -pl :commandapi-bukkit-nms-common -am -P Platform.Bukkit,"$2" --quiet

    # Find the spigot-mapped jar
    for jar in "$BUILD_DIR"/CommandAPI-*.jar; do
        if [[ "$jar" != *"original-"* && "$jar" == *").jar" ]]; then
            echo "Found JAR: $jar"
            cp "$jar" "$NEW_JAR"
            break
        fi
    done

    if [ ! -e "$NEW_JAR" ]; then
        # Couldn't find the jar with NMS_Common, so compilation failed
        echo "Failed to compile $1"

        # Clean up
        rm -rf "$PREV_CLASSES"
        # Fail
        exit 1
    fi

    # Unzip jar so we can view the classes
    unzip -q "$NEW_JAR" -d "$NEW_CLASSES"
    rm "$NEW_JAR"

    # Replace all class files with their bytecode so we can compare by text
    find "$NEW_CLASSES" -name "*.class" | while read -r class; do
        class_name="${class%.class}"
        echo "Getting bytecode for $class_name"

        javap -c -p "$class" > "$class_name.txt"
        rm "$class"
    done

    if [ -d "$PREV_CLASSES" ]; then
        # Generate diff against the previous version to detect changes
        diff_file="$DIFF_DIR/$1.diff"
        git diff --no-index "$PREV_CLASSES" "$NEW_CLASSES" > "$diff_file"
        
        if [ ! -s "$diff_file" ]; then
            # diff file is empty, so no problems here
            echo "No changes in $1"
            rm "$diff_file"
        fi

        rm -r "$PREV_CLASSES"
    fi

    # Make new classes the previous classes for the next try
    mv "$NEW_CLASSES/" "$PREV_CLASSES/"
}

compileVersion "1.20 and 1.20.1" "Spigot_1_20_R1"
compileVersion "1.20.2" "Spigot_1_20_R2"
compileVersion "1.20.3 and 1.20.4" "Spigot_1_20_R3"
compileVersion "1.20.5 and 1.20.6" "Spigot_1_20_R4"
compileVersion "1.21 and 1.21.1" "Spigot_1_21_R1"
compileVersion "1.21.2 and 1.21.3" "Spigot_1_21_R2"
compileVersion "1.21.4" "Spigot_1_21_R3"
compileVersion "1.21.5" "Spigot_1_21_R4"

# Clean up
rm -rf "$PREV_CLASSES"

# Report if any diff was found
if [ -z "$(ls -A "$DIFF_DIR")" ]; then
    # Diff directory is empty, no problems :)
    echo "No differences detected"

    rm -r "$DIFF_DIR"
else
    echo "Differences found!"

    for diff in "$DIFF_DIR"/*; do
        echo "=== $diff ==="
        cat "$diff"
    done

    echo "Inspect the $DIFF_DIR directory for details"

    # fail
    exit 1
fi