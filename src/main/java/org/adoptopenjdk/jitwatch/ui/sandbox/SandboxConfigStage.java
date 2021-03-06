/*
 * Copyright (c) 2013, 2014 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.sandbox;

import org.adoptopenjdk.jitwatch.core.JITWatchConfig;
import org.adoptopenjdk.jitwatch.core.JITWatchConfig.CompressedOops;
import org.adoptopenjdk.jitwatch.core.JITWatchConfig.TieredCompilation;
import org.adoptopenjdk.jitwatch.ui.FileChooserList;
import org.adoptopenjdk.jitwatch.ui.IStageCloseListener;
import org.adoptopenjdk.jitwatch.util.DisassemblyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

public class SandboxConfigStage extends Stage
{
	private static final String DEFAULT_DISPLAY_STYLE = "-fx-padding:0px 8px 0px 0px";

	private TextField txtFreqInline;
	private TextField txtMaxInline;
	private TextField txtCompilerThreshold;

	private CheckBox checkBoxPrintAssembly;
	private CheckBox checkBoxDisableInlining;

	private IStageCloseListener parent;
	private JITWatchConfig config;
	private FileChooserList chooserClasses;
	private VMLanguageList vmLanguageList;

	private static final int labelWidth = 150;

	private static final Logger logger = LoggerFactory.getLogger(SandboxConfigStage.class);

	public SandboxConfigStage(final IStageCloseListener parent, final JITWatchConfig config)
	{
		this.parent = parent;
		this.config = config;

		initStyle(StageStyle.UTILITY);

		VBox vbox = new VBox();

		vbox.setPadding(new Insets(15));
		vbox.setSpacing(15);

		chooserClasses = new FileChooserList(this, "Compile and Runtime Classpath", config.getClassLocations());
		chooserClasses.prefHeightProperty().bind(this.heightProperty().multiply(0.25));

		vmLanguageList = new VMLanguageList("VM Languages", config);
		vmLanguageList.prefHeightProperty().bind(this.heightProperty().multiply(0.25));

		vbox.getChildren().add(chooserClasses);
		
		vbox.getChildren().add(vmLanguageList);

		vbox.getChildren().add(buildHBoxAssemblySyntax());

		vbox.getChildren().add(buildHBoxTieredCompilation());

		vbox.getChildren().add(buildHBoxCompressedOops());

		vbox.getChildren().add(buildHBoxInliningSettings());

		vbox.getChildren().add(buildHBoxCompilationThresholds());

		vbox.getChildren().add(buildHBoxButtons());

		setTitle("Sandbox Configuration");

		Scene scene = new Scene(vbox, 720, 520);

		setScene(scene);

		setOnCloseRequest(new EventHandler<WindowEvent>()
		{
			@Override
			public void handle(WindowEvent arg0)
			{
				parent.handleStageClosed(SandboxConfigStage.this);
			}
		});
	}

	private EventHandler<ActionEvent> getEventHandlerForCancelButton()
	{
		return new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent e)
			{
				parent.handleStageClosed(SandboxConfigStage.this);
				close();
			}
		};
	}

	private EventHandler<ActionEvent> getEventHandlerForSaveButton()
	{
		return new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent e)
			{
				config.setClassLocations(chooserClasses.getFiles());

				setFrequencyOfInlineSize(config);

				setMaximumInlineSize(config);

				setCompilerThreshold(config);

				config.setPrintAssembly(checkBoxPrintAssembly.isSelected());
				config.setDisableInlining(checkBoxDisableInlining.isSelected());

				config.saveConfig();

				parent.handleStageClosed(SandboxConfigStage.this);
				close();
			}
		};
	}

	private void setCompilerThreshold(JITWatchConfig config)
	{
		try
		{
			config.setCompilerThreshold(Integer.parseInt(txtCompilerThreshold.getText()));
		}
		catch (NumberFormatException nfe)
		{
			logger.error("Bad CompilerThreshold value", nfe);
		}
	}

	private void setMaximumInlineSize(JITWatchConfig config)
	{
		try
		{
			config.setMaxInlineSize(Integer.parseInt(txtMaxInline.getText()));
		}
		catch (NumberFormatException nfe)
		{
			logger.error("Bad MaxInlineSize value", nfe);
		}
	}

	private void setFrequencyOfInlineSize(JITWatchConfig config)
	{
		try
		{
			config.setFreqInlineSize(Integer.parseInt(txtFreqInline.getText()));
		}
		catch (NumberFormatException nfe)
		{
			logger.error("Bad FreqInlineSize value", nfe);
		}
	}

	private HBox buildHBoxAssemblySyntax()
	{
		final RadioButton rbATT = new RadioButton("AT&T syntax");
		final RadioButton rbIntel = new RadioButton("Intel syntax");

		rbIntel.setDisable(true);

		final ToggleGroup groupAssemblySyntax = new ToggleGroup();

		boolean intelMode = config.isSandboxIntelMode();

		rbATT.setToggleGroup(groupAssemblySyntax);
		rbIntel.setToggleGroup(groupAssemblySyntax);

		rbATT.setStyle(DEFAULT_DISPLAY_STYLE);

		rbATT.setSelected(!intelMode);
		rbIntel.setSelected(intelMode);

		groupAssemblySyntax.selectedToggleProperty().addListener(
				getChangeListenerForGroupAssemblySyntax(rbIntel, groupAssemblySyntax));

		HBox hbox = new HBox();
		
		hbox.setSpacing(20);

		hbox.getChildren().add(buildCheckBoxPrintAssembly());
		hbox.getChildren().add(rbATT);
		hbox.getChildren().add(rbIntel);

		return hbox;
	}

	private ChangeListener<Toggle> getChangeListenerForGroupAssemblySyntax(final RadioButton rbIntel,
			final ToggleGroup groupAssemblySyntax)
	{
		return new ChangeListener<Toggle>()
		{
			@Override
			public void changed(ObservableValue<? extends Toggle> arg0, Toggle arg1, Toggle arg2)
			{
				if (groupAssemblySyntax.getSelectedToggle() != null)
				{
					boolean nextIntelMode = groupAssemblySyntax.getSelectedToggle().equals(rbIntel);

					config.setSandboxIntelMode(nextIntelMode);
				}
			}
		};
	}

	private HBox buildHBoxTieredCompilation()
	{
		final RadioButton rbVMDefault = new RadioButton("VM Default");
		final RadioButton rbForceTiered = new RadioButton("-XX:+TieredCompilation");
		final RadioButton rbForceNoTiered = new RadioButton("-XX:-TieredCompilation");

		final ToggleGroup groupTiered = new ToggleGroup();

		rbVMDefault.setStyle(DEFAULT_DISPLAY_STYLE);
		rbForceTiered.setStyle(DEFAULT_DISPLAY_STYLE);

		TieredCompilation tieredMode = config.getTieredCompilationMode();

		switch (tieredMode)
		{
		case VM_DEFAULT:
			rbVMDefault.setSelected(true);
			rbForceTiered.setSelected(false);
			rbForceNoTiered.setSelected(false);
			break;
		case FORCE_TIERED:
			rbVMDefault.setSelected(false);
			rbForceTiered.setSelected(true);
			rbForceNoTiered.setSelected(false);
			break;
		case FORCE_NO_TIERED:
			rbVMDefault.setSelected(false);
			rbForceTiered.setSelected(false);
			rbForceNoTiered.setSelected(true);
			break;
		}

		rbVMDefault.setToggleGroup(groupTiered);
		rbForceTiered.setToggleGroup(groupTiered);
		rbForceNoTiered.setToggleGroup(groupTiered);

		groupTiered.selectedToggleProperty().addListener(
				getChangeListenerForGroupTiered(rbVMDefault, rbForceTiered, rbForceNoTiered, groupTiered));

		HBox hbox = new HBox();

		Label lblMode = new Label("Tiered Compilation:");
		lblMode.setMinWidth(labelWidth);

		hbox.getChildren().add(lblMode);

		hbox.getChildren().add(rbVMDefault);
		hbox.getChildren().add(rbForceTiered);
		hbox.getChildren().add(rbForceNoTiered);

		return hbox;
	}

	private ChangeListener<Toggle> getChangeListenerForGroupTiered(final RadioButton rbVMDefault, final RadioButton rbForceTiered,
			final RadioButton rbForceNoTiered, final ToggleGroup groupTiered)
	{
		return new ChangeListener<Toggle>()
		{
			@Override
			public void changed(ObservableValue<? extends Toggle> arg0, Toggle arg1, Toggle arg2)
			{
				Toggle selectedToggle = groupTiered.getSelectedToggle();

				if (selectedToggle != null)
				{
					if (selectedToggle.equals(rbForceNoTiered))
					{
						config.setTieredCompilationMode(TieredCompilation.FORCE_NO_TIERED);
					}
					else if (selectedToggle.equals(rbForceTiered))
					{
						config.setTieredCompilationMode(TieredCompilation.FORCE_TIERED);
					}
					else if (selectedToggle.equals(rbVMDefault))
					{
						config.setTieredCompilationMode(TieredCompilation.VM_DEFAULT);
					}
				}
			}
		};
	}

	private HBox buildHBoxCompressedOops()
	{
		final RadioButton rbVMDefault = new RadioButton("VM Default");
		final RadioButton rbForceCompressed = new RadioButton("-XX:+UseCompressedOops");
		final RadioButton rbForceNoCompressed = new RadioButton("-XX:-UseCompressedOops");

		final ToggleGroup groupOops = new ToggleGroup();

		rbVMDefault.setStyle(DEFAULT_DISPLAY_STYLE);
		rbForceCompressed.setStyle(DEFAULT_DISPLAY_STYLE);

		CompressedOops oopsMode = config.getCompressedOopsMode();

		switch (oopsMode)
		{
		case VM_DEFAULT:
			rbVMDefault.setSelected(true);
			rbForceCompressed.setSelected(false);
			rbForceNoCompressed.setSelected(false);
			break;
		case FORCE_COMPRESSED:
			rbVMDefault.setSelected(false);
			rbForceCompressed.setSelected(true);
			rbForceNoCompressed.setSelected(false);
			break;
		case FORCE_NO_COMPRESSED:
			rbVMDefault.setSelected(false);
			rbForceCompressed.setSelected(false);
			rbForceNoCompressed.setSelected(true);
			break;
		}

		rbVMDefault.setToggleGroup(groupOops);
		rbForceCompressed.setToggleGroup(groupOops);
		rbForceNoCompressed.setToggleGroup(groupOops);

		groupOops.selectedToggleProperty().addListener(
				getChangeListenerForGroupOops(rbVMDefault, rbForceCompressed, rbForceNoCompressed, groupOops));

		HBox hbox = new HBox();

		Label lblMode = new Label("Compressed Oops:");
		lblMode.setMinWidth(labelWidth);

		hbox.getChildren().add(lblMode);

		hbox.getChildren().add(rbVMDefault);
		hbox.getChildren().add(rbForceCompressed);
		hbox.getChildren().add(rbForceNoCompressed);

		return hbox;
	}

	private HBox buildHBoxInliningSettings()
	{
		HBox hbox = new HBox();

		hbox.setSpacing(20);

		hbox.getChildren().add(buildCheckBoxDisableInlining());
		buildHBoxFreqInline(hbox);
		buildHBoxMaxInline(hbox);

		return hbox;
	}

	private HBox buildHBoxCompilationThresholds()
	{
		HBox hbox = new HBox();

		hbox.setSpacing(20);

		txtCompilerThreshold = new TextField(Integer.toString(config.getCompilerThreshold()));
		txtCompilerThreshold.setMaxWidth(70);
		txtCompilerThreshold.setAlignment(Pos.BASELINE_RIGHT);

		Label label = new Label("-XX:CompilationThreshold:");

		hbox.getChildren().add(label);
		hbox.getChildren().add(txtCompilerThreshold);
		return hbox;

	}

	private ChangeListener<Toggle> getChangeListenerForGroupOops(final RadioButton rbVMDefault,
			final RadioButton rbForceCompressed, final RadioButton rbForceNoCompressed, final ToggleGroup groupOops)
	{
		return new ChangeListener<Toggle>()
		{
			@Override
			public void changed(ObservableValue<? extends Toggle> arg0, Toggle arg1, Toggle arg2)
			{
				Toggle selectedToggle = groupOops.getSelectedToggle();

				if (selectedToggle != null)
				{
					if (selectedToggle.equals(rbForceNoCompressed))
					{
						config.setCompressedOopsMode(CompressedOops.FORCE_NO_COMPRESSED);
					}
					else if (selectedToggle.equals(rbForceCompressed))
					{
						config.setCompressedOopsMode(CompressedOops.FORCE_COMPRESSED);
					}
					else if (selectedToggle.equals(rbVMDefault))
					{
						config.setCompressedOopsMode(CompressedOops.VM_DEFAULT);
					}
				}
			}
		};
	}

	private void buildHBoxFreqInline(HBox hbCompilerSettings)
	{
		txtFreqInline = new TextField(Integer.toString(config.getFreqInlineSize()));
		txtFreqInline.setMaxWidth(50);
		txtFreqInline.setAlignment(Pos.BASELINE_RIGHT);
		txtFreqInline.setDisable(config.isDisableInlining());

		Label label = new Label("-XX:FreqInlineSize:");

		hbCompilerSettings.getChildren().add(label);
		hbCompilerSettings.getChildren().add(txtFreqInline);
	}

	private void buildHBoxMaxInline(HBox hbCompilerSettings)
	{
		txtMaxInline = new TextField(Integer.toString(config.getMaxInlineSize()));
		txtMaxInline.setMaxWidth(50);
		txtMaxInline.setAlignment(Pos.BASELINE_RIGHT);
		txtMaxInline.setDisable(config.isDisableInlining());

		Label label = new Label("-XX:MaxInlineSize:");

		hbCompilerSettings.getChildren().add(label);
		hbCompilerSettings.getChildren().add(txtMaxInline);
	}

	private CheckBox buildCheckBoxPrintAssembly()
	{
		checkBoxPrintAssembly = new CheckBox("Disassemble native code");

		boolean checked = false;

		if (DisassemblyUtil.isDisassemblerAvailable())
		{
			if (config.isPrintAssembly())
			{
				checked = true;
			}
		}
		else
		{
			checkBoxPrintAssembly.setDisable(true);
		}

		checkBoxPrintAssembly.setSelected(checked);

		return checkBoxPrintAssembly;
	}

	private CheckBox buildCheckBoxDisableInlining()
	{
		checkBoxDisableInlining = new CheckBox("Disable Inlining (-XX:-Inline)");

		checkBoxDisableInlining.setSelected(config.isDisableInlining());

		checkBoxDisableInlining.selectedProperty().addListener(new ChangeListener<Boolean>()
		{
			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal)
			{
				txtFreqInline.setDisable(newVal);
				txtMaxInline.setDisable(newVal);
			}
		});

		return checkBoxDisableInlining;
	}

	private HBox buildHBoxButtons()
	{
		HBox hbox = new HBox();
		hbox.setSpacing(20);
		hbox.setPadding(new Insets(0,10,0,10));
		hbox.setAlignment(Pos.CENTER);

		Button btnSave = new Button("Save");
		Button btnCancel = new Button("Cancel");

		btnSave.setOnAction(getEventHandlerForSaveButton());

		btnCancel.setOnAction(getEventHandlerForCancelButton());

		hbox.getChildren().add(btnCancel);
		hbox.getChildren().add(btnSave);

		return hbox;
	}
}