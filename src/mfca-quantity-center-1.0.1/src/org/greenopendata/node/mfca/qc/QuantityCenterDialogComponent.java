package org.greenopendata.node.mfca.qc;

import javax.swing.JPanel;

import org.greenopendata.mfca.qc.QcSettingsPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Quantity Center configuration dialog component.   
 *  
 * @author Mahler Chou, Green Open Data, 2024/4/8, 5/5, 5/25
 */
public final class QuantityCenterDialogComponent extends DialogComponent {			
	private SettingsModelByteArray m_model_byte_array;
	private QcSettingsPane m_pane = new QcSettingsPane();
	
	public QuantityCenterDialogComponent(SettingsModelByteArray model) {		
		super(model);
		this.m_model_byte_array = model;
	}
	
	@Override
	public JPanel getComponentPanel() {
		return this.m_pane;
	}

	/**
	 * Restored settings for QC dialog.
	 */
	@Override
	protected void updateComponent() {
		byte[] bArr = m_model_byte_array.getByteArray();
		this.m_pane.setQcSettingsByteArray(bArr);
	}

	/**
	 * Prepare settings stream. This will be called after "Apply" or "Ok" when closing dialog.
	 */
	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		if (m_pane.validateSettings()) {
			/**
			 * Save the data stream in settings model by 
			 * replace its origin byte array.
			 */
			byte[] bArr = m_pane.getQcSettingsByteArray();		
			m_model_byte_array.setByteArray(bArr);			
		}
		else {
			throw new InvalidSettingsException("Check the settings in the configuration dialog.");
		}
	}
	
	@Override
	protected void checkConfigurabilityBeforeLoad(PortObjectSpec[] specs) throws NotConfigurableException {
	}

	@Override
	protected void setEnabledComponents(boolean enabled) {
	}

	@Override
	public void setToolTipText(String text) {
	}	
}
