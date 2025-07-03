package dev.jorel.commandapi;

import dev.jorel.commandapi.exceptions.UnsupportedVersionException;
import dev.jorel.commandapi.nms.APITypeProvider;
import dev.jorel.commandapi.nms.PaperNMS;
import dev.jorel.commandapi.nms.PaperNMS_1_20_R4;
import dev.jorel.commandapi.nms.PaperNMS_1_21_R1;
import dev.jorel.commandapi.nms.PaperNMS_1_21_R2;
import dev.jorel.commandapi.nms.PaperNMS_1_21_R3;
import dev.jorel.commandapi.nms.PaperNMS_1_21_R4;
import dev.jorel.commandapi.nms.PaperNMS_1_21_R5;
import org.bukkit.Bukkit;
import io.papermc.paper.ServerBuildInfo;

public abstract class CommandAPIVersionHandler {

	static LoadContext getPlatform() {
		return new LoadContext(new CommandAPIPaper<>());
	}

	@SuppressWarnings("ConstantValue")
	static Object getVersion() {
		String latestMajorVersion = "21"; // Change this for Minecraft's major update

		try {
			if (CommandAPI.getConfiguration().shouldUseLatestNMSVersion()) {
				return new VersionContext(new APITypeProvider(new PaperNMS_1_21_R5()), () -> {
					CommandAPI.logWarning("Loading the CommandAPI with the latest and potentially incompatible NMS implementation.");
					CommandAPI.logWarning("While you may find success with this, further updates might be necessary to fully support the version you are using.");
				});
			} else {
				ServerBuildInfo buildInfo = ServerBuildInfo.buildInfo();
				String version = buildInfo.minecraftVersionId();
				PaperNMS<?> versionAdapter = switch (version) {
					case "1.20.6" -> new PaperNMS_1_20_R4();
					case "1.21", "1.21.1" -> new PaperNMS_1_21_R1();
					case "1.21.2", "1.21.3" -> new PaperNMS_1_21_R2();
					case "1.21.4" -> new PaperNMS_1_21_R3();
					case "1.21.5" -> new PaperNMS_1_21_R4();
					case "1.21.6", "1.21.7" -> new PaperNMS_1_21_R5();
					default -> null;
				};
				if (versionAdapter != null) {
					return new VersionContext(new APITypeProvider(versionAdapter));
				}
				if (CommandAPI.getConfiguration().shouldBeLenientForMinorVersions()) {
					String currentMajorVersion = version.split("\\.")[1];
					if (latestMajorVersion.equals(currentMajorVersion)) {
						return new VersionContext(new APITypeProvider(new PaperNMS_1_21_R5()), () -> {
							CommandAPI.logWarning("Loading the CommandAPI with a potentially incompatible NMS implementation.");
							CommandAPI.logWarning("While you may find success with this, further updates might be necessary to fully support the version you are using.");
						});
					}
				}
				version = buildInfo.asString(ServerBuildInfo.StringRepresentation.VERSION_SIMPLE);
				throw new UnsupportedVersionException(version);
			}
		} catch (Throwable error) {
			if (error instanceof ClassNotFoundException cnfe) {
				// Thrown when users use versions before the ServerBuildInfo was added. We should inform them
				// to update since Paper 1.20.6 was experimental then anyway
				throw new IllegalStateException("The CommandAPI doesn't support any version before Paper 1.20.6 build 79. Please update your server!", cnfe);
			} else {
				throw error;
			}
		}
	}

}
