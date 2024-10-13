# mfca-knime-extension
Material Flow Cost Accounting (MFCA) originated in Germany in the 1990s as a management tool that examines the environmental impact and cost of resource flows in production processes. It aims to reduce resource usage, save production costs, and minimize unnecessary waste by analyzing inputs and outputs. In 2011, it became an international standard (ISO 14051) after gaining widespread adoption in Japan. MFCA quantifies process outputs in monetary terms to identify improvement and cost reduction areas, making it a powerful tool for organizations to balance economic and environmental considerations while promoting green supply chains and products.

Calculating material cost accounting can be difficult, and appropriate software tools need improvement. Manufacturing processes vary greatly, and having a standard information system covering all calculations is difficult.

With over 20 years of experience in Java programming and a deep understanding of manufacturing processes, I have found the KNIME Analytics Platform invaluable for implementing MFCA. Its visualized operation interface simplifies the reflection of the manufacturing process in a KNIME workflow, making it straightforward. Leveraging my expertise, I have created a series of MFCA KNIME NODE to promote material flow cost accounting. I am?confident in the platform's ability to support this critical aspect of manufacturing management.

Mahler Chou

## Installation

>Find the latest version of KNIME Analytics Platform from [KNIME Analytics Platform 5.3](https://www.knime.com/downloads)

+ To get started with the MFCA extension for KNIME, download the JAR file (org.greenopendata_1.0.X.jar) from the latest release. Once downloaded, copy the JAR file into the [KNIME Folder]\dropins folder, such as c:\knime_5.3.1\dropins.
+ Copy the JAR file into the folder [KNIME Folder]\dropins, such as c:\knime_5.3.1\dropins.
+ Start the KNIME (knime.exe), and you can find the Quantity Center node in the Node Repository.

## How do you use the MFCA quantity center node?

To use MFCA, you must learn about Material Flow Cost Accounting. There are quite a number of tutorials and documents. Some YouTube videos have great explanations of MFCA, and I recommend you Google it.

> For users or professionals already aware of MFCA, I have created a tutorial lesion, which can be found in docs. 
[KNIME_for_MFCA_LS1.pdf](docs%2FMFCA%20Workflow%20Lesson%201%2FKNIME_for_MFCA_LS1.pdf)

![mfca-knime-workflow-example.png](docs%2Fmfca-knime-workflow-example.png)

## About the development of the KNIME extension.

The development of this plugin (extension) relies on KNIME SDK. Anyone interested in creating a Java-based KNIME extension should read the tutorial [Create a New Java-based KNIME Extension](https://docs.knime.com/latest/analytics_platform_new_node_quickstart_guide/index.html#_introduction).
