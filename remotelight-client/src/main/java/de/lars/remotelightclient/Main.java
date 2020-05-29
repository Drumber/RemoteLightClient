/*******************************************************************************
 * ______                     _       _     _       _     _   
 * | ___ \                   | |     | |   (_)     | |   | |  
 * | |_/ /___ _ __ ___   ___ | |_ ___| |    _  __ _| |__ | |_ 
 * |    // _ \ '_ ` _ \ / _ \| __/ _ \ |   | |/ _` | '_ \| __|
 * | |\ \  __/ | | | | | (_) | ||  __/ |___| | (_| | | | | |_ 
 * \_| \_\___|_| |_| |_|\___/ \__\___\_____/_|\__, |_| |_|\__|
 *                                             __/ |          
 *                                            |___/           
 * 
 * Copyright (C) 2019 Lars O.
 * 
 * This file is part of RemoteLight.
 ******************************************************************************/
package de.lars.remotelightclient;

import java.awt.EventQueue;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.tinylog.Logger;
import org.tinylog.configuration.Configuration;
import org.tinylog.provider.ProviderRegistry;

import com.formdev.flatlaf.FlatLaf;

import de.lars.remotelightclient.ui.MainFrame;
import de.lars.remotelightclient.ui.Style;
import de.lars.remotelightclient.utils.ui.FlatLafThemesUtil;
import de.lars.remotelightclient.utils.ui.UiUtils;
import de.lars.remotelightcore.DataStorage;
import de.lars.remotelightcore.EffectManagerHelper;
import de.lars.remotelightcore.animation.AnimationManager;
import de.lars.remotelightcore.cmd.CommandParser;
import de.lars.remotelightcore.cmd.StartParameterHandler;
import de.lars.remotelightcore.devices.DeviceManager;
import de.lars.remotelightcore.lua.LuaManager;
import de.lars.remotelightcore.musicsync.MusicSyncManager;
import de.lars.remotelightcore.out.OutputManager;
import de.lars.remotelightcore.scene.SceneManager;
import de.lars.remotelightcore.screencolor.ScreenColorManager;
import de.lars.remotelightcore.settings.SettingsManager;
import de.lars.remotelightcore.settings.types.SettingSelection;
import de.lars.remotelightcore.utils.DirectoryUtil;
import de.lars.remotelightcore.utils.ExceptionHandler;

public class Main {
	private boolean shuttingDown = false;
	private static long startMillis = System.currentTimeMillis();
	
	public final static String VERSION = "pre0.2.0.9.2";
	public final static String WEBSITE = "https://remotelight-software.blogspot.com";
	public final static String GITHUB = "https://github.com/Drumber/RemoteLight";
	
	private static Main instance;
	private static boolean headless;
	public static StartParameterHandler startParameter;
	private AnimationManager aniManager;
	private SceneManager sceneManager;
	private MusicSyncManager musicManager;
	private ScreenColorManager screenColorManager;
	private DeviceManager deviceManager;
	private OutputManager outputManager;
	private SettingsManager settingsManager;
	private EffectManagerHelper effectManagerHelper;
	private LuaManager luaManager;
	private MainFrame mainFrame;
	private CommandParser commandParser;

	public static void main(String[] args) {
		startParameter = new StartParameterHandler(args);
		new Main(true);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				if(!getInstance().shuttingDown) { //prevent calling close method twice
					instance.close(false);
				}
			}
		});
	}
	
	public Main(boolean uiMode) {
		headless = !uiMode;
		this.configureLogger();			// Configure Logger (set log path etc.)
		instance = this;
		// set default exception handler
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
		setupOSSupport();
		
		// console cmd reader
		//commandParser = new CommandParser(instance);
		//commandParser.setOutputEnabled(true);
		//new ConsoleReader(commandParser);
		
		Logger.info("Starting RemoteLight version " + VERSION);
		Style.loadFonts();				// Load custom fonts
		DataStorage.start();			// Load data file
		
		settingsManager = new SettingsManager();
		settingsManager.load(DataStorage.SETTINGSMANAGER_KEY);
		deviceManager = new DeviceManager();
		outputManager = new OutputManager();
		luaManager = new LuaManager();
		
		//new StartUp(startParameter);	// Includes some things that need to be executed at startup
		
		// Instantiate the managers of the different modes
		aniManager = new AnimationManager();
		sceneManager = new SceneManager();
		musicManager = new MusicSyncManager();
		screenColorManager = new ScreenColorManager();
		effectManagerHelper = new EffectManagerHelper();
		// Start UI
		if(uiMode) {
			startMainFrame();
		}
	}
	
	/**
	 * Starts the UI
	 */
	public void startMainFrame() {
		// set look and feel
		boolean failed = !setLookAndFeel();
		if(failed) {
			try {
				UIManager.setLookAndFeel(new MetalLookAndFeel());
			} catch (UnsupportedLookAndFeelException e) {
				e.printStackTrace();
			}
		}
		Logger.info((failed ? "[FAILED] switch to standard LaF: " : "Selected Look and Feel: ") + UIManager.getLookAndFeel().getName());
		
		// register error handler in MainFrame
		ExceptionHandler.registerListener(MainFrame.onException);
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					mainFrame = new MainFrame();
					mainFrame.setVisible(!startParameter.tray);
				} catch (Exception e) {
					Logger.error(e);
				}
			}
		});
	}
	
	public boolean setLookAndFeel() {
		SettingSelection sLaF = (SettingSelection) getSettingsManager().getSettingFromId("ui.laf");
		UiUtils.setThemingEnabled(true);
		try {
			if(sLaF == null || sLaF.getSelected().equalsIgnoreCase("System default")) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				return true;
			}
			String lafName = sLaF.getSelected();
			
			if(lafName.equalsIgnoreCase("Java default")) {
				UIManager.setLookAndFeel(new MetalLookAndFeel());
				return true;
			}
			
			for(FlatLaf laf : FlatLafThemesUtil.getAllThemes()) {
				if(laf.getName().equalsIgnoreCase(lafName)) {
					FlatLaf.install(laf);
					UiUtils.setThemingEnabled(false);
					return true;
				}
			}
		} catch (InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException | ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}
	
	
	public static void setupOSSupport() {
		// MacOS menu bar application name
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("apple.awt.application.name", "RemoteLight");
	}
	
	
	public static Main getInstance() {
		return instance;
	}
	
	/**
	 * 
	 * @return true if UI mode, false if headless
	 */
	public static boolean isHeadless() {
		return headless;
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
	
	public ScreenColorManager getScreenColorManager() {
		return screenColorManager;
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
	
	public MainFrame getMainFrame() {
		return mainFrame;
	}
	
	public CommandParser getCommandParser() {
		return commandParser;
	}

	/**
	 * Returns the number of LEDs of the active output
	 * @return
	 */
	public static int getLedNum() {
		if(instance.getOutputManager().getActiveOutput() != null) {
			if(instance.getOutputManager().getActiveOutput().getOutputPatch().getClone() > 0) {
				return instance.getOutputManager().getActiveOutput().getOutputPatch().getPatchedPixelNumber();
			}
			return instance.getOutputManager().getActiveOutput().getPixels();
		}
		return 0;
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
		//System.out.println(m);
		return (int) m;
	}
	
	
	public void close(boolean autoexit) {
		shuttingDown = true;
		try {
			// save active effect as command
			String[] activeEffect = effectManagerHelper.getActiveManagerAndEffect();
			String activeCommand = null;
			if(activeEffect[0] != null && activeEffect[1] != null) {
				activeCommand = "start " + String.join(" ", activeEffect);
				Logger.info("Saving last used effect command: " + activeCommand);
			}
			settingsManager.getSettingObject("manager.lastactive.command").setValue(activeCommand);
			
			this.getEffectManagerHelper().stopAll();// Stop all active effects
			this.getOutputManager().close();		// Close active output
			
			this.getDeviceManager().saveDevices();	// Save device list
			this.getSettingsManager().save(DataStorage.SETTINGSMANAGER_KEY); // Save all settings
			
			DataStorage.save();	// Save data file
			
			//copy log file and rename
			DirectoryUtil.copyAndRenameLog(
					new File(DirectoryUtil.getLogsPath() + "log.txt"),
					new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date().getTime()) + ".txt");
			
			try {
				ProviderRegistry.getLoggingProvider().shutdown();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(autoexit) {
				Thread.sleep(450);
				System.exit(0);
			}
		} catch(Exception e) {
			e.printStackTrace();
			if(autoexit) {
				System.exit(0);
			}
		}
	}
	
	private void configureLogger() {
		new File(DirectoryUtil.getDataStoragePath()).mkdir();
		new File(DirectoryUtil.getLogsPath()).mkdir();
		Configuration.set("writerF.file", DirectoryUtil.getLogsPath() + "log.txt");
	}

}
