package org.greenopendata.node.mfca.qc;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * This is an implementation of the node dialog of the "Quantity Center" node.
 *  
 * @author Green Open Data - MAHLER CHOU (周敬斐)
 *         2024.5.25 (^;^)
 */
public class QuantityCenterNodeDialog extends NodeDialogPane {
	
	private String m_defaultTabTitle = "Quantity Center Settings";

	private final List<DialogComponent> m_dialogComponents = new ArrayList<DialogComponent>();

	private JPanel m_compositePanel;

	private JPanel m_currentPanel;

	private SettingsModelByteArray m_settingsModel;	

	/**
	 * New dialog pane for configuring the node. The dialog created here
	 * will show up when double clicking on a node in KNIME Analytics Platform.
	 */
    protected QuantityCenterNodeDialog() {
        super();
        
        m_compositePanel = new JPanel();
		m_compositePanel.setLayout(new BoxLayout(m_compositePanel, BoxLayout.Y_AXIS));
		m_currentPanel = m_compositePanel;
		super.addTab(m_defaultTabTitle, m_compositePanel);            
		
		this.m_settingsModel = QuantityCenterNodeModel.createByteArraySettingsModel();

		QuantityCenterDialogComponent qcPaneComponent = new QuantityCenterDialogComponent(this.m_settingsModel);
						
		m_dialogComponents.add(qcPaneComponent);
		m_currentPanel.add(qcPaneComponent.getComponentPanel());
    }
    
	/**
	 * Invoked before the dialog window is opened. The settings object passed,
	 * contains the current settings of the corresponding node model. The model
	 * and the dialog must agree on a mutual contract on how settings are stored
	 * in the spec. I.e. they must able to read each other's settings.
	 * <p>
	 * The implementation must be able to handle invalid or incomplete settings
	 * as the model may not have any reasonable values yet (for example when the
	 * dialog is opened for the first time). When an empty/invalid settings
	 * object is passed the dialog should set default value.
	 *
	 * @param settings the <code>NodeSettings</code> to read from
	 * @param specs    the input specs
	 * @throws NotConfigurableException if the node can currently not be configured
	 */
	@Override
	public final void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
		assert settings != null;
		assert specs != null;

		for (DialogComponent comp : m_dialogComponents) {
			comp.loadSettingsFrom(settings, specs);
		}

		loadAdditionalSettingsFrom(settings, specs);
	}

	/**
	 * Save settings of all registered <code>DialogComponents</code> into the
	 * configuration object.
	 *
	 * @param settings the <code>NodeSettings</code> to write into
	 * @throws InvalidSettingsException if the user has entered wrong values
	 */
	@Override
	public final void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		for (DialogComponent comp : m_dialogComponents) {
			comp.saveSettingsTo(settings);
		}

		saveAdditionalSettingsTo(settings);
	}

	/**
	 * This method can be overridden to load additional settings. Override this
	 * method if you have mixed input types (different port types). Alternatively,
	 * if your node only has ordinary data inputs, consider to overwrite the
	 * {@link #loadAdditionalSettingsFrom(NodeSettingsRO, DataTableSpec[])} method,
	 * which does the type casting already.
	 *
	 * @param settings the <code>NodeSettings</code> to read from
	 * @param specs    the input specs
	 * @throws NotConfigurableException if the node can currently not be configured
	 */
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
			throws NotConfigurableException {
		DataTableSpec[] dtsArray = new DataTableSpec[specs.length];
		boolean canCallDTSMethod = true;
		for (int i = 0; i < dtsArray.length; i++) {
			PortObjectSpec s = specs[i];
			if (s instanceof DataTableSpec) {
				dtsArray[i] = (DataTableSpec) s;
			} else if (s == null) {
				dtsArray[i] = new DataTableSpec();
			} else {
				canCallDTSMethod = false;
			}
		}
		if (canCallDTSMethod) {
			loadAdditionalSettingsFrom(settings, dtsArray);
		}
	}

	/**
	 * Override hook to load additional settings when all input ports are data
	 * ports. This method is the specific implementation to
	 * {@link #loadAdditionalSettingsFrom(NodeSettingsRO, PortObjectSpec[])} if all
	 * input ports are data ports. All elements in the <code>specs</code> argument
	 * are guaranteed to be non-null.
	 * 
	 * @param settings The settings of the node
	 * @param specs The <code>DataTableSpec</code> of the input tables.
	 * @throws NotConfigurableException If not configurable
	 */
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
			throws NotConfigurableException {
	}

	/**
	 * This method can be overridden to save additional settings to the given
	 * settings object.
	 *
	 * @param settings the <code>NodeSettings</code> to write into
	 * @throws InvalidSettingsException if the user has entered wrong values
	 */
	public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		assert settings != null;
	}	
}