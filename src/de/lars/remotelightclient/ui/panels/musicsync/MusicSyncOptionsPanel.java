package de.lars.remotelightclient.ui.panels.musicsync;

import javax.swing.JPanel;
import javax.swing.JRadioButton;

import de.lars.remotelightclient.Main;
import de.lars.remotelightclient.musicsync.InputUtil;
import de.lars.remotelightclient.musicsync.MusicSyncManager;
import de.lars.remotelightclient.musicsync.sound.Shared;
import de.lars.remotelightclient.musicsync.sound.SoundProcessing;
import de.lars.remotelightclient.settings.Setting;
import de.lars.remotelightclient.settings.SettingsManager;
import de.lars.remotelightclient.settings.types.SettingBoolean;
import de.lars.remotelightclient.settings.types.SettingColor;
import de.lars.remotelightclient.settings.types.SettingDouble;
import de.lars.remotelightclient.settings.types.SettingInt;
import de.lars.remotelightclient.settings.types.SettingObject;
import de.lars.remotelightclient.settings.types.SettingSelection;
import de.lars.remotelightclient.settings.types.SettingString;
import de.lars.remotelightclient.ui.Style;
import de.lars.remotelightclient.ui.panels.settings.settingComps.SettingBooleanPanel;
import de.lars.remotelightclient.ui.panels.settings.settingComps.SettingColorPanel;
import de.lars.remotelightclient.ui.panels.settings.settingComps.SettingDoublePanel;
import de.lars.remotelightclient.ui.panels.settings.settingComps.SettingIntPanel;
import de.lars.remotelightclient.ui.panels.settings.settingComps.SettingPanel;
import de.lars.remotelightclient.ui.panels.settings.settingComps.SettingPanel.SettingChangedListener;
import de.lars.remotelightclient.ui.panels.settings.settingComps.SettingSelectionPanel;
import de.lars.remotelightclient.ui.panels.settings.settingComps.SettingStringPanel;
import de.lars.remotelightclient.utils.UiUtils;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JSlider;
import java.awt.GridLayout;
import javax.swing.border.EmptyBorder;

public class MusicSyncOptionsPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -100112066309451958L;
	private List<SettingPanel> settingPanels;
	private SettingsManager sm = Main.getInstance().getSettingsManager();
	private MusicSyncManager msm = Main.getInstance().getMusicSyncManager();
	private JPanel bgrEffectOptions;
	private JPanel bgrEffectOptionsScroll;
	private JPanel panelSensitivity;
	private JLabel lblSensitivity;
	private JSlider sliderSensitivity;
	private JPanel panelAdjustment;
	private JLabel lblAdjustment;
	private JSlider sliderAdjustment;
	private JPanel panelInput;
	private JScrollPane scrollPaneOpt;
	private JPanel bgrOptions;
	private JLabel lblInput;

	/**
	 * Create the panel.
	 */
	public MusicSyncOptionsPanel() {
		settingPanels = new ArrayList<SettingPanel>();
		sm.addSetting(new SettingObject("musicsync.sensitivity", null, 20));
		sm.addSetting(new SettingObject("musicsync.adjustment", null, 200));
		
		Dimension size = new Dimension(Integer.MAX_VALUE, 150);
		//setPreferredSize(size);
		//setMaximumSize(size);
		setBackground(Style.panelDarkBackground);
		setAlignmentY(Component.TOP_ALIGNMENT);
		
		setLayout(new GridLayout(0, 2, 0, 0));
		JPanel bgrScrollOptions = new JPanel();
		bgrScrollOptions.setBorder(new EmptyBorder(4, 4, 0, 0));
		bgrScrollOptions.setLayout(new BoxLayout(bgrScrollOptions, BoxLayout.Y_AXIS));
		bgrScrollOptions.setAlignmentY(Component.TOP_ALIGNMENT);
		bgrScrollOptions.setAlignmentX(Component.LEFT_ALIGNMENT);
		bgrScrollOptions.setBackground(Style.panelDarkBackground);
		add(bgrScrollOptions);
		
		scrollPaneOpt = new JScrollPane();
		scrollPaneOpt.setViewportBorder(null);
		scrollPaneOpt.setBorder(BorderFactory.createEmptyBorder());
		scrollPaneOpt.getVerticalScrollBar().setUnitIncrement(8);
		bgrScrollOptions.add(scrollPaneOpt);
		
		bgrOptions = new JPanel();
		bgrOptions.setLayout(new BoxLayout(bgrOptions, BoxLayout.Y_AXIS));
		bgrOptions.setBackground(Style.panelDarkBackground);
		scrollPaneOpt.setViewportView(bgrOptions);
		
		panelSensitivity = new JPanel();
		panelSensitivity.setLayout(new BoxLayout(panelSensitivity, BoxLayout.Y_AXIS));
		panelSensitivity.setAlignmentX(Component.LEFT_ALIGNMENT);
		panelSensitivity.setAlignmentY(Component.TOP_ALIGNMENT);
		panelSensitivity.setBackground(Style.panelDarkBackground);
		bgrOptions.add(panelSensitivity);
		
		lblSensitivity = new JLabel("Sensitivity");
		lblSensitivity.setAlignmentX(Component.LEFT_ALIGNMENT);
		lblSensitivity.setForeground(Style.textColor);
		panelSensitivity.add(lblSensitivity);
		
		size = new Dimension(180, 20);
		
		sliderSensitivity = new JSlider();
		sliderSensitivity.setPreferredSize(size);
		sliderSensitivity.setMaximumSize(size);
		sliderSensitivity.setMinimum(10);
		sliderSensitivity.setMaximum(300);
		sliderSensitivity.setFocusable(false);
		sliderSensitivity.setAlignmentX(Component.LEFT_ALIGNMENT);
		sliderSensitivity.setBackground(Style.panelDarkBackground);
		sliderSensitivity.setName("sensitivity");
		sliderSensitivity.addChangeListener(sliderListener);
		UiUtils.addSliderMouseWheelListener(sliderSensitivity);
		sliderSensitivity.setValue((int) sm.getSettingObject("musicsync.sensitivity").getValue());
		panelSensitivity.add(sliderSensitivity);
		
		panelAdjustment = new JPanel();
		panelAdjustment.setLayout(new BoxLayout(panelAdjustment, BoxLayout.Y_AXIS));
		panelAdjustment.setAlignmentY(Component.TOP_ALIGNMENT);
		panelAdjustment.setAlignmentX(Component.LEFT_ALIGNMENT);
		panelAdjustment.setBackground(Style.panelDarkBackground);
		bgrOptions.add(panelAdjustment);
		
		lblAdjustment = new JLabel("Adjustment");
		lblAdjustment.setAlignmentX(Component.LEFT_ALIGNMENT);
		lblAdjustment.setForeground(Style.textColor);
		panelAdjustment.add(lblAdjustment);
		
		sliderAdjustment = new JSlider();
		sliderAdjustment.setMinimum(50);
		sliderAdjustment.setMaximum(700);
		sliderAdjustment.setPreferredSize(size);
		sliderAdjustment.setMaximumSize(size);
		sliderAdjustment.setFocusable(false);
		sliderAdjustment.setAlignmentX(Component.LEFT_ALIGNMENT);
		sliderAdjustment.setBackground(Style.panelDarkBackground);
		sliderAdjustment.setName("adjustment");
		sliderAdjustment.addChangeListener(sliderListener);
		UiUtils.addSliderMouseWheelListener(sliderAdjustment);
		sliderAdjustment.setValue((int) sm.getSettingObject("musicsync.adjustment").getValue());
		panelAdjustment.add(sliderAdjustment);
		
		panelInput = new JPanel();
		panelInput.setAlignmentY(Component.TOP_ALIGNMENT);
		panelInput.setAlignmentX(Component.LEFT_ALIGNMENT);
		panelInput.setBackground(Style.panelDarkBackground);
		bgrOptions.add(panelInput);
		panelInput.setLayout(new BorderLayout(0, 0));
		
		bgrEffectOptionsScroll = new JPanel();
		bgrEffectOptionsScroll.setVisible(false);
		add(bgrEffectOptionsScroll);
		bgrEffectOptionsScroll.setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setViewportBorder(null);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		bgrEffectOptionsScroll.add(scrollPane);
		
		bgrEffectOptions = new JPanel();
		bgrEffectOptions.setBorder(new EmptyBorder(4, 4, 0, 0));
		bgrEffectOptions.setBackground(Style.panelDarkBackground);
		scrollPane.setViewportView(bgrEffectOptions);
		bgrEffectOptions.setLayout(new BoxLayout(bgrEffectOptions, BoxLayout.Y_AXIS));
		
		this.initInputPanel();
	}
	
	
	private ChangeListener sliderListener = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			JSlider slider = (JSlider) e.getSource();
			
			if(slider.getName().equals("sensitivity")) {
				sm.getSettingObject("musicsync.sensitivity").setValue(slider.getValue());
				msm.setSensitivity(slider.getValue() / 100.0);
				
			} else if(slider.getName().equals("adjustment")) {
				sm.getSettingObject("musicsync.adjustment").setValue(slider.getValue());
				msm.setAdjustment(slider.getValue() / 100.0);
			}
		}
	};
	
	
	public void addMusicEffectOptions(List<Setting> settings) {
		bgrEffectOptionsScroll.setVisible(true);
		bgrEffectOptions.removeAll();
		
		JLabel lblTitle = new JLabel("Effect options", SwingConstants.LEFT);
		lblTitle.setFont(Style.getFontBold(11));
		lblTitle.setForeground(Style.accent);
		lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
		bgrEffectOptions.add(lblTitle);
		
		for(Setting s : settings) {
			SettingPanel spanel = this.getSettingPanel(s);
			spanel.setAlignmentX(Component.LEFT_ALIGNMENT);
			spanel.setSettingChangedListener(effectOptionsChangeListener);
			spanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
			spanel.setBackground(Style.panelDarkBackground);
			bgrEffectOptions.add(spanel);
			settingPanels.add(spanel);
		}
		updateUI();
	}
	
	public void removeMusicEffectOptions() {
		bgrEffectOptions.removeAll();
		bgrEffectOptionsScroll.setVisible(false);
		updateUI();
	}
	
	
	private SettingPanel getSettingPanel(Setting s) {
		if(s instanceof SettingString) {
			return new SettingStringPanel((SettingString) s);
		}
		if(s instanceof SettingBoolean) {
			return new SettingBooleanPanel((SettingBoolean) s);
		}
		if(s instanceof SettingColor) {
			return new SettingColorPanel((SettingColor) s);
		}
		if(s instanceof SettingDouble) {
			return new SettingDoublePanel((SettingDouble) s);
		}
		if(s instanceof SettingInt) {
			return new SettingIntPanel((SettingInt) s);
		}
		if(s instanceof SettingSelection) {
			return new SettingSelectionPanel((SettingSelection) s);
		}
		return null;
	}
	
	private SettingChangedListener effectOptionsChangeListener = new SettingChangedListener() {
		@Override
		public void onSettingChanged(Setting setting) {
			for(SettingPanel sp : settingPanels) {
				sp.setValue();
			}
		}
	};
	
	private void initInputPanel() {
		String input = (String) sm.getSettingObject("musicsync.input").getValue();
		
		JPanel buttonPanel = new JPanel(new GridLayout(0,1));
		buttonPanel.setBackground(Style.panelDarkBackground);
		ButtonGroup group = new ButtonGroup();
		
		for(Mixer.Info info : Shared.getMixerInfo(false, true)) {
			Mixer mixer = AudioSystem.getMixer(info);

			if(InputUtil.isLineSupported(mixer)) {
				JRadioButton button = new JRadioButton();
				button.setBackground(Style.panelDarkBackground);
				button.setForeground(Style.textColor);
				button.setText(Shared.toLocalString(info));
				buttonPanel.add(button);
				group.add(button);
				button.setActionCommand(info.toString());
				button.addActionListener(inputSelectedListener);
				//set last time input as selected
				if(input != null) {
					if(input.equals(info.toString())) {
						button.setSelected(true);
					}
				}
			}
		}
		
		lblInput = new JLabel("Select a input");
		lblInput.setForeground(Style.textColor);
		panelInput.add(lblInput, BorderLayout.NORTH);
		panelInput.add(buttonPanel);
	}
	
	private ActionListener inputSelectedListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			for(Mixer.Info info : Shared.getMixerInfo(false, true)) {
				if(e.getActionCommand().equals(info.toString())){
					Mixer newMixer = AudioSystem.getMixer(info);
					SoundProcessing.setMixer(newMixer);
					//save last selected to data file
					sm.getSettingObject("musicsync.input").setValue(info.toString());
					//refresh SoundProcessor
					Main.getInstance().getMusicSyncManager().newSoundProcessor();
					break;
				}
			}
		}
	};
	
}
