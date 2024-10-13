package org.greenopendata.node.mfca.qc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.greenopendata.mfca.qc.QcCalcRowItem;
import org.greenopendata.mfca.qc.QcRawItem;
import org.greenopendata.mfca.qc.QcRawItemType;
import org.greenopendata.mfca.qc.QcSettingsModel;
import org.greenopendata.mfca.qc.mi.MaterialInputRowItem;
import org.greenopendata.mfca.qc.po.ProductOutputRowItem;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

/**
 * This class defining Quantity Center node's configuration.
 *  
 * @author Mahler Chou 2024.5.6
 */
public final class QuantityCenterNodeModel extends NodeModel {
    
    /**
	 * The logger is used to print info/warning/error messages to the KNIME console
	 * and to the KNIME log file. Retrieve it via 'NodeLogger.getLogger' providing
	 * the class of this node model.
	 */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(QuantityCenterNodeModel.class);

	/**
	 * The configuration name of Quantity Center node.
	 */
	private static final String KEY_CNFG_QC = "quantity_center_settings";
	
	/**
	 * The byte array settings model holds the information in dialog which will be gathered 
	 * in QcSettingsModel class.
	 */
	private SettingsModelByteArray m_settings = createByteArraySettingsModel();
	
	/**
	 * This is the QC settings pane model that will be marshaled from m_settings byte array.
	 * Due to different stage of process, the object state changes.
	 */
	private QcSettingsModel m_qc_model = null;
		
	/**
	 * Constructor for the node model.
	 */
	protected QuantityCenterNodeModel() {
		/**
		 * There are four output ports and one input port. 
		 * Input port  : Material Input (Optional)
		 * Output ports: Product Output, Negative Output, Calculation and Raw Data.
		 */
		super(createInPorts(), createOutPorts());		
	}
	
	/**
	 * In-Port would be material input port (optional)
	 */
	private static PortType[] createInPorts() {
		PortType optionalType = PortTypeRegistry.getInstance().getPortType(BufferedDataTable.class, true);
		PortType[] inPortList = new PortType[1];
		inPortList[0] = optionalType;
		return inPortList;
	}

	/**
	 * In-Port would be material for output port.
	 */
	private static PortType[] createOutPorts() {
		PortType requiredPortType = PortTypeRegistry.getInstance().getPortType(BufferedDataTable.class, false);
		PortType[] outPortList = new PortType[4];
		Arrays.fill(outPortList, requiredPortType);
		return outPortList;
	}

	/**
	 * Create a default settings model for Quantity Center.
	 * @return a new SettingsModelQuantityCenter with the default settings.
	 */
	static SettingsModelByteArray createByteArraySettingsModel() {
		return new SettingsModelByteArray(KEY_CNFG_QC);
	}

	/**
	 * NODE CONFIGURATION 
	 * 
	 * The input port for material input is optional.
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		/*
		 * Check if the node is executable, e.g. all required user settings are
		 * available and valid, or the incoming types are feasible for the node to
		 * execute. In case the node can execute in its current configuration with the
		 * current input, calculate and return the table spec that would result of the
		 * execution of this node. I.e. this method precalculates the table spec of the
		 * output table.
		 */ 
		if (inSpecs != null && inSpecs.length > 0) {
			DataTableSpec inTableSpec = inSpecs[0];
			if ( inTableSpec != null ) {
				//: Check for material input table specs.
				DataTableSpec materialInputTableSpec = createMaterialInputTableSpec();
				String[] columnNames = materialInputTableSpec.getColumnNames();
				for(String colName : columnNames) {
					if (inTableSpec.findColumnIndex(colName) < 0) {
						throw new InvalidSettingsException("A material input table must have column " + colName + ".");	
					}
				}
			}			
		}
		
		/*
		 * Creating the output table specification for output ports.
		 */
		DataTableSpec[] outputTableSpec = new DataTableSpec[4];
		outputTableSpec[0] = createProductOutputTableSpec();
		outputTableSpec[1] = createNegativeOutputTableSpec();
		outputTableSpec[2] = createCalculationTableSpec();
		outputTableSpec[3] = createRawDataTableSpec();
				
		return outputTableSpec;
	}
	
	/**
	 * NODE EXECUTION
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception 
	{
		/*
		 * The input data table to work with. The "inData" array will contain as many
		 * input tables as specified in the constructor. In this case it can only be one
		 * (see constructor).
		 */
		BufferedDataTable inputData = inData[0];
		
		/*
		 * Create the spec of the output table, for each double column of the input
		 * table we will create one formatted String column in the output. See the
		 * javadoc of the "createOutputSpec(...)" for more information.
		 */
		DataTableSpec[] outputTableSpec = new DataTableSpec[4];
		outputTableSpec[0] = createProductOutputTableSpec();
		outputTableSpec[1] = createNegativeOutputTableSpec();
		outputTableSpec[2] = createCalculationTableSpec();
		outputTableSpec[3] = createRawDataTableSpec();		
		
		/*
		 * The execution context provides storage capacity, in this case a
		 * data container to which we will add rows sequentially. Note, this container
		 * can handle arbitrary big data tables, it will buffer to disc if necessary.
		 * The execution context is provided as an argument to the execute method by the
		 * framework. Have a look at the methods of the "exec". There is a lot of
		 * functionality to create and change data tables.
		 */
		BufferedDataContainer[] containers = new BufferedDataContainer[4];
		for(int i=0; i<containers.length; ++i) {
			containers[i] = exec.createDataContainer(outputTableSpec[i]);
		}		
				
		if (this.m_qc_model == null) {
			/*
			 * Means m_qc_model instance not yet prepared by any configuration done by user.  
			 */
			throw new RuntimeException("Please configure the node before execute!");
		}
		else {			
			/*
			 * Starting the calculation of MFCA. 
			 */
			LOGGER.info("Executing QC node for process " + m_qc_model.getProcess() + ".");	

			/*
			 * The inData[0] is material input from previous QC, this would blend into calculation.
			 * But notice that "Material" in QC setting pane is static, but inData[0] is dynamic. 
			 */
			final ArrayList<MaterialInputRowItem> dynamicMtlInputList = new ArrayList<>();
			if (inputData != null) {
				/*
				 * Get the row iterator over the input table which returns each row one-by-one
				 * from the input table.
				 */
				CloseableRowIterator rowIterator = inputData.iterator();

				/*
				 * A counter for how many rows have already been processed. This is used to
				 * calculate the progress of the node, which is displayed as a loading bar under
				 * the node icon.
				 */				
				while (rowIterator.hasNext()) {
					DataRow currentRow = rowIterator.next();

					MaterialInputRowItem materialRowItem = new MaterialInputRowItem();
					
					//: Make it into QC material data row.
					StringCell partNoCell = (StringCell) currentRow.getCell(0);
					StringCell partNameCell = (StringCell) currentRow.getCell(1);
					StringCell unitCell = (StringCell) currentRow.getCell(2);
					DoubleCell priceCell = (DoubleCell) currentRow.getCell(3);
					DoubleCell quantityCell = (DoubleCell) currentRow.getCell(4);
					
					materialRowItem.setSource("dynamic");
					materialRowItem.setPartNo(partNoCell.getStringValue());
					materialRowItem.setPartName(partNameCell.getStringValue());
					materialRowItem.setUnit(unitCell.getStringValue());
					materialRowItem.setUnitPrice(priceCell.getDoubleValue());
					materialRowItem.setStdUsage(1.0);
					materialRowItem.setYield(1.0);
					materialRowItem.setActUsage(quantityCell.getDoubleValue());
					
					dynamicMtlInputList.add(materialRowItem);

					exec.checkCanceled();	//: Check if user hit cancel.
				}				
			}
			
			/*
			 * Output Port 0 - Positive Product
			 */
			{
				List<ProductOutputRowItem> list0 = this.m_qc_model.getProductOutputRowItems();
				int i=0;
				for(ProductOutputRowItem r : list0) {
					ArrayList<DataCell> cells = new ArrayList<DataCell>();
					
					cells.add(new StringCell(r.getPartNo()));
					cells.add(new StringCell(r.getPartName()));
					cells.add(new StringCell(r.getUnit()));
					cells.add(new DoubleCell(r.getUnitPrice()));
					cells.add(new DoubleCell(r.getQuantityPass()));
					cells.add(new DoubleCell(r.getUnitPrice() * r.getQuantityPass()));
					 
					DataRow new_row = new DefaultRow(i + "", cells);
					containers[0].addRowToTable(new_row);
					++i;
				}					
				exec.setProgress(0.25);
			}

			/*
			 * Output Port 1 - Negative Loss
			 */
			{
				List<ProductOutputRowItem> list0 = this.m_qc_model.getProductOutputRowItems();
				int i=0;
				for(ProductOutputRowItem r : list0) {
					ArrayList<DataCell> cells = new ArrayList<DataCell>();
					
					cells.add(new StringCell(r.getPartNo()));
					cells.add(new StringCell(r.getPartName()));
					cells.add(new StringCell(r.getUnit()));
					cells.add(new DoubleCell(r.getUnitPrice()));
					cells.add(new DoubleCell(r.getQuantityNG()));
					cells.add(new DoubleCell(r.getQuantityNG() * r.getUnitPrice()));
					 
					DataRow new_row = new DefaultRow(i + "", cells);
					containers[1].addRowToTable(new_row);
					++i;
				}					
				exec.setProgress(0.25);
			}

			/*
			 * Output Port 2 - Calculations
			 */
			{
				/* GET THE CALCULATION FROM QC NODE, THE DYNAMIC MTL. FROM INPUT PORT */
				ArrayList<QcCalcRowItem> calcResult = m_qc_model.calculate(dynamicMtlInputList);
				
				int i=0;
				for(QcCalcRowItem r : calcResult) {
					ArrayList<DataCell> cells = new ArrayList<DataCell>();
					
					cells.add(new StringCell(r.getProcess()));
					cells.add(new DoubleCell(r.getMaterial_input()));
					cells.add(new DoubleCell(r.getMaterial_cost()));
					cells.add(new DoubleCell(r.getEnergy_cost()));
					cells.add(new DoubleCell(r.getSystem_cost()));
					cells.add(new DoubleCell(r.getWaste_quantity()));
					cells.add(new DoubleCell(r.getWaste_cost()));
					cells.add(new DoubleCell(r.getPositive_product()));
					cells.add(new DoubleCell(r.getPositive_output_cost()));
					cells.add(new DoubleCell(r.getNagative_loss()));
					cells.add(new DoubleCell(r.getNagative_loss_cost()));
					cells.add(new DoubleCell(r.getBalance()));
					
					DataRow new_row = new DefaultRow(i + "", cells);
					containers[2].addRowToTable(new_row);
					++i;
				}									
			}
			
			/*
			 * Output Port 3 - Raw Items
			 */
			{
				int rowID=0;

				//: Item from inTable (ie. Dynamic Material Input)
				if (inputData != null) {
					CloseableRowIterator rowIterator = inputData.iterator();
					while(rowIterator.hasNext()) {
						DataRow row = rowIterator.next();
						
						ArrayList<DataCell> cells = new ArrayList<DataCell>();
						
						cells.add(new StringCell(m_qc_model.getProcess()));
						cells.add(new StringCell(QcRawItemType.DYNAMIC_MATERIAL_INPUT));
						
						cells.add(new StringCell(((StringCell) row.getCell(0)).getStringValue()));			// part_no
						cells.add(new StringCell(((StringCell) row.getCell(1)).getStringValue()));			// part_name
						cells.add(new StringCell(((StringCell) row.getCell(2)).getStringValue()));			// unit
						cells.add(new DoubleCell(((DoubleCell) row.getCell(4)).getDoubleValue()));			// quantity
						cells.add(new DoubleCell(((DoubleCell) row.getCell(3)).getDoubleValue()));			// unit_price
						cells.add(new DoubleCell(
								((DoubleCell) row.getCell(3)).getDoubleValue() *
								((DoubleCell) row.getCell(4)).getDoubleValue()
						));																					// monetary
						
						DataRow new_row = new DefaultRow(rowID + "", cells);
						containers[3].addRowToTable(new_row);
	
						++rowID;
					}
					}
				
				//: Raw Item from QC Node
				List<QcRawItem> list = this.m_qc_model.getRawItems();
				for(QcRawItem r : list) {
					ArrayList<DataCell> cells = new ArrayList<DataCell>();
					
					cells.add(new StringCell(r.getProcess()));
					cells.add(new StringCell(r.getType()));
					cells.add(new StringCell(r.getItemNo()));
					cells.add(new StringCell(r.getItemName()));
					cells.add(new StringCell(r.getUnit()));
					cells.add(new DoubleCell(r.getQuantity()));
					cells.add(new DoubleCell(r.getUnit_price()));
					cells.add(new DoubleCell(r.getMonetary()));
					 
					DataRow new_row = new DefaultRow(rowID + "", cells);
					containers[3].addRowToTable(new_row);
					++rowID;
				}									
			}								
		}
		
		/*
		 * Once we are done, we close the container and return its table. Here we need
		 * to return as many tables as we specified in the constructor. This node has
		 * one output, hence return one table (wrapped in an array of tables).
		 * 
		 * Remember that container close first and then get data table.
		 */		
		BufferedDataTable[] outputTables = new BufferedDataTable[4];
		for(int i=0; i<containers.length; ++i) {
			containers[i].close();
			outputTables[i] = containers[i].getTable();
		}
		return outputTables;
	}


	/**
	 * This is called when opening workflow from KNIME or close dialog after configuration.
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		/*
		 * Save user settings to the NodeSettings object. SettingsModels already know how to
		 * save them self to a NodeSettings object by calling the below method. In general,
		 * the NodeSettings object is just a key-value store and has methods to write
		 * all common data types. Hence, you can easily write your settings manually.
		 * See the methods of the NodeSettingsWO.
		 */				
		this.m_settings.saveSettingsTo(settings);
		System.err.println("saveSettingsTo");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		/*
		 * Load (valid) settings from the NodeSettings object. It can be safely assumed that
		 * the settings are validated by the method below.
		 * 
		 * The SettingsModel will handle the loading. After this call, the current value
		 * (from the view) can be retrieved from the settings model.
		 */
		this.m_settings.loadSettingsFrom(settings);
	
		/**
		 * Marshal save byte array into object. This method will be called after user clicking 
		 * node setting dialog "APPLY" or "OK" button.
		 */
		final byte[] bArr = this.m_settings.getByteArray();
		this.m_qc_model = QcSettingsModel.fromByteArray(bArr);		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		/*
		 * Check if the settings could be applied to our model e.g. if the user provided
		 * format String is empty. In this case we do not need to check as this is
		 * already handled in the dialog. Do not actually set any values of any member
		 * variables.
		 */
		this.m_settings.validateSettings(settings);
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		/*
		 * Advanced method, usually left empty. Everything that is
		 * handed to the output ports is loaded automatically (data returned by the execute
		 * method, models loaded in loadModelContent, and user settings set through
		 * loadSettingsFrom - is all taken care of). Only load the internals
		 * that need to be restored (e.g. data used by the views).
		 */
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		/*
		 * Advanced method, usually left empty. Everything
		 * written to the output ports is saved automatically (data returned by the execute
		 * method, models saved in the saveModelContent, and user settings saved through
		 * saveSettingsTo - is all taken care of). Save only the internals
		 * that need to be preserved (e.g. data used by the views).
		 */
	}

	@Override
	protected void reset() {
		/*
		 * Code executed on a reset of the node. Models built during execute are cleared
		 * and the data handled in loadInternals/saveInternals will be erased.
		 */
	}
	
	/*==============================================================================================
	 * Local helper functions 
	 *==============================================================================================*/

	/**
	 * DataTableSpec for Material Input.
	 * @author Mahler Chou
	 * @return Material Input data table spec.
	 */
	private DataTableSpec createMaterialInputTableSpec() {
		List<DataColumnSpec> columnSpecs = new ArrayList<>();

		columnSpecs.add((new DataColumnSpecCreator("part_no", StringCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("part_name", StringCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("unit", StringCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("unit_price", DoubleCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("quantity", DoubleCell.TYPE)).createSpec());

		DataColumnSpec[] newColumnSpecsArray = columnSpecs.toArray(new DataColumnSpec[columnSpecs.size()]);		
		return new DataTableSpec(newColumnSpecsArray);		
	}
	
	/**
	 * DataTableSpec for Positive Output, this table should be viewed as material input to consequence node.
	 * @author Mahler Chou
	 * @return
	 */
	private DataTableSpec createProductOutputTableSpec() {
		List<DataColumnSpec> columnSpecs = new ArrayList<>();

		columnSpecs.add((new DataColumnSpecCreator("part_no", StringCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("part_name", StringCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("unit", StringCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("unit_price", DoubleCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("quantity", DoubleCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("subtotal", DoubleCell.TYPE)).createSpec());

		DataColumnSpec[] newColumnSpecsArray = columnSpecs.toArray(new DataColumnSpec[columnSpecs.size()]);		
		return new DataTableSpec(newColumnSpecsArray);
	}
		
	private DataTableSpec createNegativeOutputTableSpec() {
		List<DataColumnSpec> columnSpecs = new ArrayList<>();

		columnSpecs.add((new DataColumnSpecCreator("part_no", StringCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("part_name", StringCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("unit", StringCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("unit_price", DoubleCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("quantity", DoubleCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("subtotal", DoubleCell.TYPE)).createSpec());

		DataColumnSpec[] newColumnSpecsArray = columnSpecs.toArray(new DataColumnSpec[columnSpecs.size()]);		
		return new DataTableSpec(newColumnSpecsArray);		
	}	 
	
	/**
	 * The calculation table reveals the calculation results.
	 */
	private DataTableSpec createCalculationTableSpec() {
		List<DataColumnSpec> columnSpecs = new ArrayList<>();

		columnSpecs.add((new DataColumnSpecCreator("process", StringCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("material_input", DoubleCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("material_cost", DoubleCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("energy_cost", DoubleCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("system_cost", DoubleCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("waste_quantity", DoubleCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("waste_cost", DoubleCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("positive_output", DoubleCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("positive_output_cost", DoubleCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("negative_loss", DoubleCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("negative_loss_cost", DoubleCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("balance", DoubleCell.TYPE)).createSpec());

		DataColumnSpec[] newColumnSpecsArray = columnSpecs.toArray(new DataColumnSpec[columnSpecs.size()]);		
		return new DataTableSpec(newColumnSpecsArray);		
	}	 

	/**
	 * This will output the raw data for further data or report processing.
	 */
	private DataTableSpec createRawDataTableSpec() {
		List<DataColumnSpec> columnSpecs = new ArrayList<>();

		columnSpecs.add((new DataColumnSpecCreator("process", StringCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("type", StringCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("item_no", StringCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("item_name", StringCell.TYPE)).createSpec());		
		columnSpecs.add((new DataColumnSpecCreator("unit", StringCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("quantity", DoubleCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("unit_price", DoubleCell.TYPE)).createSpec());
		columnSpecs.add((new DataColumnSpecCreator("monetary", DoubleCell.TYPE)).createSpec());

		DataColumnSpec[] newColumnSpecsArray = columnSpecs.toArray(new DataColumnSpec[columnSpecs.size()]);		
		return new DataTableSpec(newColumnSpecsArray);		
	}
}

