echo "Compiling NMS_Common for Spigot 1.20 and 1.20.1..."
mvn clean package -Dmaven.source.skip=true -Dmaven.javadoc.skip=true -pl :commandapi-bukkit-nms-common -am -P Platform.Bukkit,Spigot_1_20_R1 --quiet

echo "Compiling NMS_Common for Spigot 1.20.2..."
mvn clean package -Dmaven.source.skip=true -Dmaven.javadoc.skip=true -pl :commandapi-bukkit-nms-common -am -P Platform.Bukkit,Spigot_1_20_R2 --quiet

echo "Compiling NMS_Common for Spigot 1.20.3 and 1.20.4..."
mvn clean package -Dmaven.source.skip=true -Dmaven.javadoc.skip=true -pl :commandapi-bukkit-nms-common -am -P Platform.Bukkit,Spigot_1_20_R3 --quiet

echo "Compiling NMS_Common for Spigot 1.20.5 and 1.20.6..."
mvn clean package -Dmaven.source.skip=true -Dmaven.javadoc.skip=true -pl :commandapi-bukkit-nms-common -am -P Platform.Bukkit,Spigot_1_20_R4 --quiet

echo "Compiling NMS_Common for Spigot 1.21 and 1.21.1..."
mvn clean package -Dmaven.source.skip=true -Dmaven.javadoc.skip=true -pl :commandapi-bukkit-nms-common -am -P Platform.Bukkit,Spigot_1_21_R1 --quiet

echo "Compiling NMS_Common for Spigot 1.21.2 and 1.21.3..."
mvn clean package -Dmaven.source.skip=true -Dmaven.javadoc.skip=true -pl :commandapi-bukkit-nms-common -am -P Platform.Bukkit,Spigot_1_21_R2 --quiet

echo "Compiling NMS_Common for Spigot 1.21.4..."
mvn clean package -Dmaven.source.skip=true -Dmaven.javadoc.skip=true -pl :commandapi-bukkit-nms-common -am -P Platform.Bukkit,Spigot_1_21_R3 --quiet

echo "Compiling NMS_Common for Spigot 1.21.5..."
mvn clean package -Dmaven.source.skip=true -Dmaven.javadoc.skip=true -pl :commandapi-bukkit-nms-common -am -P Platform.Bukkit,Spigot_1_21_R4 --quiet