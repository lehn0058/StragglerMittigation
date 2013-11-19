package mapreduce;

import java.io.Console;
import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapreduce.Cluster;

public class Status
{
	private JobClient _client;
	private RunningJob _runningJob;
	private vSphereClient _vSphereClient;
	
	public Status(JobClient client, RunningJob runningJob)
	{
		_client = client;
		_runningJob = runningJob;
	}
	
	// TODO: Analysis should be done for reducers and mappers separately.
	// This is because reducers are long lived and are not necessarily straggling
	// just because they are running longer than map tasks.
	public void Monitor() throws Exception
	{					
		// Probe for stragglers while the job is running
		while (!_runningJob.isComplete())
		{
			// Wait for a time before continuing
			Thread.sleep(5000);

			// Retrieve the latest task progress
			JobStatus jobStatus = _runningJob.getJobStatus();
						
			//ArrayList<TaskReport> allTaskReports = RetrieveAllTaskReports(jobStatus.getJobID());
			ArrayList<TaskReport> allTaskReports = RetrieveAllMapTaskReports(jobStatus.getJobID());
			
			// Determine which tasks are straggling
			ArrayList<TaskReport> stragglers = FindStragglers(allTaskReports);

			System.out.println("Number of completed tasks: " + NumberOfCompletedTasks(allTaskReports));
			System.out.println("Straggling tasks found: " + stragglers.size());
			
			// Find task reports per machine
			ArrayList<MachineTaskReport> machineTaskReports = FindMachineTaskReport(stragglers);
			
			// Check each straggler to see if they are CPU/RAM bound
			for (MachineTaskReport machineTaskReport : machineTaskReports) 
			{
				if (IsCPUBound(machineTaskReport)) AddCPUIfAvailable(machineTaskReport);
				if (IsRAMBound(machineTaskReport)) AddRAMIfAvailable(machineTaskReport);
			}
		}
	}
	
	private ArrayList<MachineTaskReport> FindMachineTaskReport(ArrayList<TaskReport> taskReports)
	{
		// NOTE: Ideally, machine information would be related to the TaskReport. This will require a change to the Hadoop API.
		// For the moment, we will just be using a single node cluster. So, we assume this is running on that single machine.
		ArrayList<MachineTaskReport> machineTaskReports = new ArrayList<MachineTaskReport>();
		for (TaskReport taskReport : taskReports)
		{
			machineTaskReports.add(new MachineTaskReport("ubuntu-master", taskReport));
		}
		
		return machineTaskReports;
		
//		Collection<TaskTrackerStatus> taskTrackerStatuses = _jobTracker.activeTaskTrackers();
//		ArrayList<MachineTaskReport> machineTaskReports = new ArrayList<MachineTaskReport>();
//		
//		// Find the matching taskID in the task reports and task tracker
//		for (TaskReport taskReport : taskReports)
//		{
//			// For each task tracker (1 per machine)
//			for (TaskTrackerStatus taskTrackerStatus : taskTrackerStatuses)
//			{				
//			     String hostName = taskTrackerStatus.getHost();
//			     System.out.println("Searching task tracker with host name: " + hostName);
//			     
//			     // Get the status of all tasks for the current host
//			     List<TaskStatus> taskStatuses = taskTrackerStatus.getTaskReports();
//			     for (TaskStatus taskStatus : taskStatuses)
//			     {
//			          TaskAttemptID taskAttemptId = taskStatus.getTaskID();
//			          			          
//			          if (taskReport.getTaskID().equals(taskAttemptId.getTaskID()))
//			          {
//			        	  System.out.println("Found taskID match!");
//			        	  
//			        	  // TODO: Group these by host name, so we don't have to de-dupe which machines are getting resources.
//			        	  // Could also be used to help calculate priority...
//			        	  machineTaskReports.add(new MachineTaskReport(hostName, taskReport));
//			          }
//			     }
//			}
//		}
//		
//		System.out.println("MachineTaskReports size: " + machineTaskReports.size());
//		
//		return machineTaskReports;
	}

	private static ArrayList<TaskReport> FindStragglers(ArrayList<TaskReport> taskReports)
	{
		// Take a time stamp to use for all calculations so they are compared evenly.
		Date now = new Date();
		
		// Filter out tasks that have not started yet
		ArrayList<TaskReportSnapshot> startedTasks = new ArrayList<TaskReportSnapshot>();
		for (TaskReport taskReport : taskReports)
		{
			if (taskReport.getStartTime() != 0)
			{
				startedTasks.add(new TaskReportSnapshot(taskReport, now));
			}
		}
		
		// Get the mean runtime
		float meanRuntime = MeanRunTime(startedTasks);
		System.out.println("Mean runtime: " + meanRuntime);
		
		// Find all tasks that are straggling
		ArrayList<TaskReport> stragglingTasks = new ArrayList<TaskReport>();
		for (TaskReportSnapshot taskReportSnapshot : startedTasks)
		{
			// If the task is not complete AND it's runtime is more than the
			// mean runtime, then it is a straggler			
			if (taskReportSnapshot.getTaskReport().getProgress() != 1 && (taskReportSnapshot.getTaskRunTime() > meanRuntime))
			{
				stragglingTasks.add(taskReportSnapshot.getTaskReport());
			}
		}

		return stragglingTasks;
	}

	// Retrieves all map tasks
	private ArrayList<TaskReport> RetrieveAllMapTaskReports(JobID jobId) throws IOException
	{
		ArrayList<TaskReport> taskReports = new ArrayList<TaskReport>();
		
		for (TaskReport report : _client.getMapTaskReports(jobId))
		{
			taskReports.add(report);
		}

		return taskReports;
	}
	
	// Retrieves all map and reduce tasks
	private ArrayList<TaskReport> RetrieveAllTaskReports(JobID jobId) throws IOException
	{				
		ArrayList<TaskReport> taskReports = new ArrayList<TaskReport>();
		
		for (TaskReport report : _client.getMapTaskReports(jobId))
		{
			taskReports.add(report);
		}

		for (TaskReport report : _client.getReduceTaskReports(jobId))
		{
			taskReports.add(report);
		}

		return taskReports;
	}
	
	// Retrieves the number of tasks that have completed.
	private static int NumberOfCompletedTasks(ArrayList<TaskReport> taskReports)
	{
		int numberOfCompletedTasks = 0;
		for (TaskReport taskReport : taskReports)
		{
			if (taskReport.getFinishTime() != 0)
			{
				numberOfCompletedTasks++;
			}
		}
		
		return numberOfCompletedTasks;
	}

	// Get the average runtime of tasks that have started.
	private static float MeanRunTime(ArrayList<TaskReportSnapshot> taskReportSnapshots)
	{
		// If there are no task reports, return 0 as the mean runtime.
		if (taskReportSnapshots.size() == 0)
		{
			return 0;
		}
		
		float totalRuntime = 0.0f;
		int numberOfStartedTasks = 0;
		for (TaskReportSnapshot taskReportSnapshot : taskReportSnapshots)
		{
			// Enforce that we don't include any tasks that are not yet started.
			if (taskReportSnapshot.getStartTime() == 0)
			{				
				continue;
			}
			
			numberOfStartedTasks++;
			totalRuntime += taskReportSnapshot.getTaskRunTime();
		}
		
		return totalRuntime / numberOfStartedTasks;
	}

	// Retrieves an open connection to the hypervisor. Since establishing the connection
	// is expensive, we keep a reference to the connected client around after its first use.
	private vSphereClient getClient()
	{
		if (_vSphereClient == null) _vSphereClient = new vSphereClient();
		return _vSphereClient;
	}
	
	// Checks to see if a task is currently CPU bound
	private boolean IsCPUBound(MachineTaskReport machineTaskReport)
	{
		ResourceUtilization resourceUtilization = getClient().retrieveMachineUtilization(machineTaskReport.getHostName());
		System.out.println("CPU utilization: " + resourceUtilization.getCpuUtilization() * 100 + "%");
		return resourceUtilization.getCpuUtilization() > 0.9 ? true : false;
	}

	// Adds a CPU to a task if one is available in the resource pool
	private void AddCPUIfAvailable(MachineTaskReport machineTaskReport)
	{
		//throw new NotImplementedException();
	}

	// Checks to see if a task is currently RAM bound
	private boolean IsRAMBound(MachineTaskReport machineTaskReport)
	{
		ResourceUtilization resourceUtilization = getClient().retrieveMachineUtilization(machineTaskReport.getHostName());
		System.out.println("Memory utilization: " + resourceUtilization.getMemoryUtilization() * 100 + "%");
		return resourceUtilization.getMemoryUtilization() > 0.9 ? true : false;
	}

	// Adds 1 GB of RAM to a task if it is available in the resource pool
	private void AddRAMIfAvailable(MachineTaskReport machineTaskReport)
	{
		//throw new NotImplementedException();
	}
}









