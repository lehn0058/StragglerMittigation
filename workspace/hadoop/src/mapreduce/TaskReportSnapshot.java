package mapreduce;

import java.util.Date;

import org.apache.hadoop.mapred.TaskReport;

/**
 * Contains runtime statistics at a snapshot in time for a task. This is used
 * so runtime values are not changing while calculations are being done. 
 *
 */
public class TaskReportSnapshot
{
	private TaskReport _taskReport;
	private long _startTime;
	private long _finishTime;
	private long _taskRunTime;
		
	public TaskReportSnapshot(TaskReport taskReport, Date now)
	{
		_taskReport = taskReport;
		_startTime = _taskReport.getStartTime();
		_finishTime = _taskReport.getFinishTime();
		_taskRunTime = TimeElapsedForTask(now);
	}
	
	public TaskReport getTaskReport()
	{
		return _taskReport;
	}
	
	public long getStartTime()
	{
		return _startTime;
	}
	
	public long getFinishTime()
	{
		return _finishTime;
	}
	
	public long getTaskRunTime()
	{
		return _taskRunTime;
	}
	
	// Calculates how long a task has run, in milliseconds
	private long TimeElapsedForTask(Date now)
	{				
		// If the task has not completed yet, its finish time may be zero
		if (_finishTime == 0)
		{
			System.out.println("Running task elapsed time: " + (now.getTime() - _startTime));	
			return now.getTime() - _startTime;
		}
		else
		{
			long timeElapsedForTask = _finishTime - _startTime;		
			return timeElapsedForTask;
		}
	}
}
