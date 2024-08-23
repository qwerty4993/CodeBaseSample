# CodeBaseSample
[ImageDimensionsFromByteArray](https://github.com/qwerty4993/CodeBaseSample/blob/develop/ImageDimensionsFromByteArray.java)
To get the image Height and Width from the byte array

[ExCustomFilter.java] (https://github.com/qwerty4993/CodeBaseSample/blob/develop/ExCustomFilter.java)
Example Configuration and Custom Filter
Explanation
	•	Custom Filter Logic: The custom filter class dynamically filters out stack trace elements based on criteria defined in the code. You can customize the filtering logic as per your requirements.
	•	Dynamic Filtering: This approach avoids hardcoding package names in the Log4j configuration file, making the filtering logic more flexible and maintainable.
	•	Registration: The custom converter is registered in the log4j2.xml using the packages attribute to specify the package containing the custom converter.

[LogProcessor2.java] (https://github.com/qwerty4993/CodeBaseSample/blob/develop/LogProcessor2.java)
Certainly! Below is the equivalent Java code for post-processing log files to extract only the exception class from each log entry. This code reads a log file, applies a regular expression to match and extract the exception class, and writes the processed output to a new file.
