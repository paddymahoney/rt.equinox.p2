package org.eclipse.equinox.internal.frameworkadmin.equinox;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.frameworkadmin.equinox.utils.FileUtils;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.osgi.service.log.LogService;

public class ParserUtils {
	public static File getOSGiInstallArea(List programArgs, LauncherData launcherData) {
		if (launcherData == null)
			return null;

		//TODO This is not enough because if you only have -startup then osgi.install.area from the config.ini is used
		File result = getOSGiInstallArea(programArgs, launcherData.getLauncher().getParentFile().toURI());
		if (result != null)
			return result;

		if (launcherData.getFwJar() != null)
			return fromOSGiJarToOSGiInstallArea(launcherData.getFwJar().getAbsolutePath());
		if (launcherData.getLauncher() != null)
			return launcherData.getLauncher().getParentFile();
		return null;
	}

	public static URI getFrameworkJar(List lines, URI launcherFolder) {
		String fwk = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_FW, lines);
		if (fwk == null) {
			//Search the file system using the default location
			URI location = FileUtils.getEclipsePluginFullLocation(EquinoxConstants.FW_SYMBOLIC_NAME, new File(URIUtil.toFile(launcherFolder), EquinoxConstants.PLUGINS_DIR));
			if (location != null)
				return location;
			return null;
		}
		try {
			return URIUtil.makeAbsolute(URIUtil.fromString(fwk), launcherFolder);
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, "can't make absolute of:" + fwk);
			return null;
		}
	}

	//This method should only be used to determine the osgi install area when reading the eclipse.ini
	public static File getOSGiInstallArea(List args, URI base) {
		if (args == null)
			return null;
		String install = getValueForArgument(EquinoxConstants.OPTION_INSTALL, args);
		if (install != null)
			return new File(install);
		String startup = getValueForArgument(EquinoxConstants.OPTION_STARTUP, args);
		if (startup != null) {
			return URIUtil.toFile(URIUtil.makeAbsolute(fromOSGiJarToOSGiInstallArea(startup).toURI(), base));
		}
		return null;
	}

	private static File fromOSGiJarToOSGiInstallArea(String path) {
		IPath parentFolder = new Path(path).removeLastSegments(1);
		if (parentFolder.lastSegment().equalsIgnoreCase("plugins")) //$NON-NLS-1$
			return parentFolder.removeLastSegments(1).toFile();
		return parentFolder.toFile();
	}

	public static boolean isArgumentSet(String arg, List args) {
		if (arg == null || args == null)
			return false;
		for (int i = 0; i < args.size(); i++) {
			if (args.get(i) == null)
				continue;
			if (((String) args.get(i)).equalsIgnoreCase(arg)) {
				return true;
			}
		}
		return false;
	}

	public static String getValueForArgument(String arg, List args) {
		if (arg == null || args == null)
			return null;
		for (int i = 0; i < args.size(); i++) {
			if (args.get(i) == null)
				continue;
			if (((String) args.get(i)).equalsIgnoreCase(arg)) {
				if (i + 1 < args.size() && args.get(i + 1) != null && ((String) args.get(i + 1)).charAt(1) != '-')
					return (String) args.get(i + 1);
			}
		}
		return null;
	}

	public static String[] getMultiValuedArgument(String arg, List args) {
		if (arg == null || args == null)
			return null;
		ArrayList values = null;
		for (int i = 0; i < args.size(); i++) {
			if (args.get(i) == null)
				continue;
			if (arg.equalsIgnoreCase((String) args.get(i))) {
				values = new ArrayList();
				continue;
			}
			if (values != null && ((String) args.get(i)).charAt(1) == '-') {
				break;
			}
			if (values != null)
				values.add(((String) args.get(i)).trim());
		}
		if (values != null)
			return (String[]) values.toArray(new String[values.size()]);
		return null;
	}

	public static boolean setValueForArgument(String arg, String value, List args) {
		if (arg == null || args == null)
			return false;
		for (int i = 0; i < args.size(); i++) {
			if (args.get(i) == null)
				continue;
			String currentArg = ((String) args.get(i)).trim();
			if (currentArg.equalsIgnoreCase(arg)) {
				if (i + 1 < args.size() && args.get(i + 1) != null && ((String) args.get(i + 1)).charAt(1) != '-') {
					args.set(i + 1, value);
					return true;
				}
			}
		}
		return false;
	}

	public static boolean removeArgument(String arg, List args) {
		if (arg == null || args == null)
			return false;
		for (int i = 0; i < args.size(); i++) {
			if (args.get(i) == null)
				continue;
			String currentArg = ((String) args.get(i)).trim();
			if (currentArg.equalsIgnoreCase(arg)) {
				args.set(i, null);
				while (i + 1 < args.size() && args.get(i + 1) != null && ((String) args.get(i + 1)).charAt(1) != '-') {
					args.set(i + 1, null);
					i++;
				}
			}
		}
		return false;
	}
}
