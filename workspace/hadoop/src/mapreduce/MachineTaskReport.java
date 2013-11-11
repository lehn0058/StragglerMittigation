package mapreduce;

import org.apache.hadoop.mapred.*;

/**
 * Relates a task report and a virtual machine
 *
 */
public class MachineTaskReport
{
	private String _hostName;
	private TaskReport _taskReport;
	
	public MachineTaskReport(String hostName, TaskReport taskReport)
	{
		setHostName(hostName);
		setTaskReport(taskReport);
	}

	public String getHostName()
	{
		return _hostName;
	}

	public void setHostName(String _hostName)
	{
		this._hostName = _hostName;
	}

	public TaskReport getTaskReport()
	{
		return _taskReport;
	}

	public void setTaskReport(TaskReport _taskReport)
	{
		this._taskReport = _taskReport;
	}
}
