package org.greenopendata.node.mfca.qc;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Setting Model for byte array. This class extends SettingsModel for
 * holding byte array. 
 * 
 * @author Mahler Chou 2024.5.6
 */
public final class SettingsModelByteArray extends SettingsModel {	
	private String m_configName;

	private byte[] m_array;
	
	public SettingsModelByteArray(String configName) {
		super();
		this.m_configName = configName;
	}
	
	public SettingsModelByteArray(String configName, final byte[] byteArray) {
		this(configName);
		this.m_array = byteArray.clone();
	}
	
	@Override
	protected String getConfigName() {
		return m_configName;
	}
	
	public byte[] getByteArray() {
		return this.m_array;
	}
	
	public void setByteArray(final byte[] byteArray) {
		this.m_array = byteArray.clone();
		notifyChangeListeners();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsForDialog(NodeSettingsRO settings, PortObjectSpec[] specs) throws NotConfigurableException {				
		if (settings.containsKey(m_configName) ) {
			setByteArray(settings.getByteArray(m_configName, null));
			notifyChangeListeners();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsForDialog(NodeSettingsWO settings) throws InvalidSettingsException {
		saveSettingsForModel(settings);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettingsForModel(NodeSettingsRO settings) throws InvalidSettingsException {
		// may not need for byte array.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsForModel(NodeSettingsRO settings) throws InvalidSettingsException {
		if (settings.containsKey(m_configName)) {
			byte[] arr = settings.getByteArray(m_configName);
			if (arr != null) {
				this.m_array = arr.clone();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsForModel(NodeSettingsWO settings) {
		if (m_array != null) {
			settings.addByteArray(m_configName, m_array);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + " ('" + m_configName + "')";
	}

	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected SettingsModelByteArray createClone() {
		return new SettingsModelByteArray(m_configName, m_array);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getModelTypeID() {
		return this.m_configName;
	}	
}
