/*-
 * >===license-start
 * RemoteLight
 * ===
 * Copyright (C) 2019 - 2020 Lars O.
 * ===
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * <===license-end
 */

package de.lars.remotelightcore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.tinylog.Logger;
import org.tinylog.configuration.Configuration;
import org.tinylog.provider.ProviderRegistry;

import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;

import de.lars.remotelightcore.animation.AnimationManager;
import de.lars.remotelightcore.cmd.CommandParser;
import de.lars.remotelightcore.cmd.ConsoleReader;
import de.lars.remotelightcore.cmd.StartParameterHandler;
import de.lars.remotelightcore.colors.ColorManager;
import de.lars.remotelightcore.colors.palette.PaletteLoader;
import de.lars.remotelightcore.devices.DeviceManager;
import de.lars.remotelightcore.event.EventHandler;
import de.lars.remotelightcore.event.events.Stated.State;
import de.lars.remotelightcore.event.events.types.ShutdownEvent;
import de.lars.remotelightcore.io.AutoSave;
import de.lars.remotelightcore.io.FileStorage;
import de.lars.remotelightcore.lua.LuaManager;
import de.lars.remotelightcore.musicsync.MusicSyncManager;
import de.lars.remotelightcore.notification.Notification;
import de.lars.remotelightcore.notification.NotificationManager;
import de.lars.remotelightcore.notification.NotificationType;
import de.lars.remotelightcore.out.Output;
import de.lars.remotelightcore.out.OutputManager;
import de.lars.remotelightcore.scene.SceneManager;
import de.lars.remotelightcore.screencolor.AbstractScreenColorManager;
import de.lars.remotelightcore.settings.SettingsManager;
import de.lars.remotelightcore.utils.DirectoryUtil;
import de.lars.remotelightcore.utils.ExceptionHandler;

public class RemoteLightCore {
	
	private static boolean shuttingDown = false;
	private static long startMillis = System.currentTimeMillis();
	
	public final static String VERSION = "v0.2.5.2";
	public final static boolean DEVBUILD = false;
	public final static String WEBSITE = "https://remotelight-software.blogspot.com";
	public final static String GITHUB = "https://github.com/Drumber/RemoteLight";
	public final static String WIKI = "https://github.com/Drumber/RemoteLight/wiki";
	public final static String DISCORD = "https://discord.gg/JcsKm5U";
	
	private static RemoteLightCore instance;
	private static boolean headless;
	public  static StartParameterHandler startParameter;
	
	private DeviceManager deviceManager;
	private OutputManager outputManager;
	private SettingsManager settingsManager;
	private NotificationManager notificationManager;
	private EventHandler eventHandler;
	
	private EffectManagerHelper effectManagerHelper;
	private AnimationManager aniManager;
	private SceneManager sceneManager;
	private MusicSyncManager musicManager;
	private AbstractScreenColorManager screenColorManager;
	private LuaManager luaManager;
	private ColorManager colorManager;
	
	private FileStorage fileStorage;
	private AutoSave fileAutoSaver;
	private CommandParser commandParser;
	
	public RemoteLightCore(String[] args, boolean headless) {
		if(instance != null) {
			throw new IllegalStateException("RemoteLightCore is already initialized!");
		}
		
		if(args == null)
			args = new String[0];
		
		instance = RemoteLightCore.this;
		startParameter = new StartParameterHandler(args);
		RemoteLightCore.headless = headless;
		
		this.configureLogger();	// configure logger (set log path etc.)
		instance = this;
		Logger.info("Starting RemoteLightCore version " + VERSION);
		
		// set default exception handler
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
		
		// init notification manager and event handler
		notificationManager = new NotificationManager();
		eventHandler = new EventHandler();
		
		// create data file
		File dataFile = new File(DirectoryUtil.getDataStoragePath() + DirectoryUtil.FILE_STORAGE_NAME);
		// instantiate file storage
		fileStorage = new FileStorage(dataFile);
		try {
			// try to load data file
			fileStorage.load();
		} catch (IOException | JsonParseException e) {
			Logger.error(e, "Could not load data file: " + (dataFile != null ? dataFile.getAbsolutePath() : "'null'"));
			showErrorNotification(e, "Data File Error. Please report on GitHub.");
		}
		
		// initialize AutoSaver
		fileAutoSaver = new AutoSave(fileStorage);
		
		updateDataFile(); // backwards compatibility to versions < v0.2.2
		
		PaletteLoader.getInstance().load();
		
		settingsManager = new SettingsManager(fileStorage);
		settingsManager.load(fileStorage.KEY_SETTINGS_LIST);
		deviceManager = new DeviceManager();
		outputManager = new OutputManager();
		luaManager = new LuaManager();
		colorManager = new ColorManager();
		
		// instantiate the managers of the different modes
		aniManager = new AnimationManager();
		sceneManager = new SceneManager();
		musicManager = new MusicSyncManager();
		effectManagerHelper = new EffectManagerHelper();
		
		// load devices
		deviceManager.loadDevices(fileStorage, fileStorage.KEY_DEVICES_LIST);
		
		// console cmd reader
		commandParser = new CommandParser(instance);
		commandParser.setOutputEnabled(true);
		new ConsoleReader(commandParser);
		
		new SetupHelper(instance); // includes some things that need to be executed at startup
	}
	
	public static RemoteLightCore getInstance() {
		if(instance == null)
			throw new IllegalStateException("RemoteLightCore is not initialized!");
		return instance;
	}
	
	public void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				if(!shuttingDown) { //prevent calling close method twice
					instance.close(false);
				}
			}
		});
	}
	
	/**
	 * Check if RemoteLight is used headless
	 * @return true if headless, false if UI mode
	 */
	public static boolean isHeadless() {
		return headless;
	}
	
	public static boolean isMacOS() {
		return System.getProperty("os.name").contains("Mac OS");
	}
	
	public AnimationManager getAnimationManager() {
		return aniManager;
	}
	
	public SceneManager getSceneManager() {
		return sceneManager;
	}
	
	public MusicSyncManager getMusicSyncManager() {
		return musicManager;
	}
	
	public AbstractScreenColorManager getScreenColorManager() {
		return screenColorManager;
	}
	
	public void setScreenColorManager(AbstractScreenColorManager screenColorManager) {
		this.screenColorManager = screenColorManager;
		effectManagerHelper.updateManagers();
	}
	
	public EffectManagerHelper getEffectManagerHelper() {
		return effectManagerHelper;
	}
	
	public DeviceManager getDeviceManager() {
		return deviceManager;
	}
	
	public OutputManager getOutputManager() {
		return outputManager;
	}
	
	public SettingsManager getSettingsManager() {
		return settingsManager;
	}
	
	public LuaManager getLuaManager() {
		return luaManager;
	}
	
	public ColorManager getColorManager() {
		return colorManager;
	}
	
	public FileStorage getFileStorage() {
		return fileStorage;
	}
	
	public AutoSave getAutoSaver() {
		return fileAutoSaver;
	}
	
	public CommandParser getCommandParser() {
		return commandParser;
	}
	
	public NotificationManager getNotificationManager() {
		return notificationManager;
	}
	
	public EventHandler getEventHandler() {
		return eventHandler;
	}

	/**
	 * Returns the number of LEDs of the active output
	 * @return
	 */
	public static int getLedNum() {
		Output out = instance.getOutputManager().getActiveOutput();
		if(out != null) {
			if(out.getOutputPatch().getClone() > 0) {
				return out.getOutputPatch().getPatchedPixelNumber();
			}
			return out.getPixels();
		}
		// return not 0 to prevent ArithmeticException or ArrayIndexOutOfBoundsException which
		// which can occur for some effects
		return OutputManager.MIN_PIXELS;
	}
	
	/**
	 * Returns the elapsed time in milliseconds since the program started as int value
	 * @return elapsed time in ms
	 */
	public static int getMillis() {
		long m = System.currentTimeMillis() - startMillis;
		if(m >= Integer.MAX_VALUE - 1) {	// reset on overflow
			m = 0;
			startMillis = System.currentTimeMillis();
		}
		return (int) m;
	}
	
	public void showNotification(Notification notification) {
		NotificationManager manager = getNotificationManager();
		manager.addNotification(notification);
	}
	
	public void showNotification(NotificationType type, String message, int displayTime) {
		Notification notification = new Notification(type, message);
		notification.setDisplayTime(displayTime);
		showNotification(notification);
	}
	
	public void showErrorNotification(Throwable e) {
		Notification notification = new Notification(NotificationType.ERROR, "An error has occurred: " + e.getClass().getCanonicalName());
		notification.setDisplayTime(Notification.LONG);
		showNotification(notification);
	}
	
	public void showErrorNotification(Throwable e, String title) {
		Notification notification = new Notification(NotificationType.ERROR, title, "An error has occurred: " + e.getClass().getCanonicalName());
		notification.setDisplayTime(Notification.LONG);
		showNotification(notification);
	}
	
	
	protected void updateDataFile() {
		File file = new File(DirectoryUtil.getDataStoragePath() + DirectoryUtil.DATA_FILE_NAME);
		if(!file.exists())
			return;
		Logger.debug("Found old data file ('" + file.getAbsolutePath() + "'). Trying to update to new Json file storage...");
		
		// backup old data file
		File backupOldFile = new File(file.getAbsolutePath() + ".old_" + VERSION);
		if(!backupOldFile.exists()) {
			try {
				Files.copy(file.toPath(), backupOldFile.toPath());
			} catch (IOException e) {
				Logger.error(e, "Could not backup old data file.");
			}
		}
		
		// delete old data file
		if(!file.delete()) {
			Logger.warn("Could not delete old data file ('" + file.getAbsolutePath() + "'). Please remove manually.");
		}
	}
	
	
	public void close(boolean autoexit) {
		shuttingDown = true;
		ShutdownEvent event = eventHandler.call(new ShutdownEvent(State.PRE));
		if(event.isCancelled()) {
			Logger.warn("The shutdown routine was cancelled by an event.");
			return;
		}
		
		try {
			// save active effect as command
			String[] activeEffect = effectManagerHelper.getActiveManagerAndEffect();
			String activeCommand = null;
			if(activeEffect[0] != null && activeEffect[1] != null) {
				activeCommand = "start " + String.join(" ", activeEffect);
				Logger.info("Saving last used effect command: " + activeCommand);
			} else if(colorManager.isActive()) {
				activeCommand = "color " + colorManager.getLastColorHex();
				Logger.info("Saving last used color as command: " + activeCommand);
			}
			settingsManager.getSettingObject("manager.lastactive.command").setValue(activeCommand);
			
			this.getEffectManagerHelper().stopAll();// Stop all active effects
			this.getOutputManager().close();		// Close active output
			
			this.getDeviceManager().saveDevices(fileStorage, fileStorage.KEY_DEVICES_LIST);	// Save device list
			this.getSettingsManager().save(fileStorage.KEY_SETTINGS_LIST); // Save all settings
			
			// save color palettes
			PaletteLoader.getInstance().store();
			
			try {
				// save data file
				fileStorage.save();
			} catch (IOException | JsonIOException e) {
				Logger.error(e, "Could not save data file: " + fileStorage.getFile());
			}
			
			// copy log file and rename
			DirectoryUtil.copyAndRenameLog(
					new File(DirectoryUtil.getLogsPath() + "log.txt"),
					new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date().getTime()) + ".txt");
			
			eventHandler.call(new ShutdownEvent(State.POST));
			instance = null;
			
			try {
				ProviderRegistry.getLoggingProvider().shutdown();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if(autoexit) {
				System.exit(0);
			}
		} catch(Exception e) {
			e.printStackTrace();
			instance = null;
			if(autoexit) {
				System.exit(0);
			}
		}
	}
	
	private void configureLogger() {
		new File(DirectoryUtil.getDataStoragePath()).mkdir();
		new File(DirectoryUtil.getLogsPath()).mkdir();
		Configuration.set("writerF.file", DirectoryUtil.getLogsPath() + "log.txt");
		if(DEVBUILD)
			Configuration.set("writerC.format", "[{date: HH:mm:ss}] [{level}] [{thread}] ({line}){class-name}.{method}(): {message}");
	}

}
