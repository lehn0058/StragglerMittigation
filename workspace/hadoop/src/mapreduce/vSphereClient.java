package mapreduce;

import com.vmware.vim25.mo.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * Handles connections and interactions with the vSphere API
 */
public class vSphereClient
{
	private ServiceInstance _si;
	
	// Connect to the default hypervisor
	public vSphereClient()
	{
		this("https://192.168.1.150/sdk", "root", "Hadoop-123");
	}
	
	// Connect to a specific hypervisor using the given username and password.
	public vSphereClient(String url, String username, String password)
	{		
		// Connect to the hypervisor server
		try
		{
			_si = new ServiceInstance(new URL(url), username, password, true);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		}
	}
	
	// Retrieves the current resource utilization for a specific virtual machine
	public ResourceUtilization retrieveMachineUtilization(String virtualMachineName)
	{
		try
		{
			// Retrieve all virtual machines
			ManagedEntity[] virtualMachines = new InventoryNavigator(_si.getRootFolder()).searchManagedEntities("VirtualMachine");
		
			// Find the virtual machine with the given name
			for (ManagedEntity managedEntity : virtualMachines)
			{
				VirtualMachine virtualMachine = (VirtualMachine) managedEntity;
				if (virtualMachine.getName().equalsIgnoreCase(virtualMachineName))
				{
					// Return the resource utilization.
					int maxCpuUsage = virtualMachine.getRuntime().getMaxCpuUsage();
					int currentCpuUsage = virtualMachine.getSummary().getQuickStats().getOverallCpuUsage();
					float currentCpuUtilization = (float)currentCpuUsage / (float)maxCpuUsage;
					
					int maxMemoryUsage = virtualMachine.getRuntime().getMaxMemoryUsage();
					int currentMemoryUsage = virtualMachine.getSummary().getQuickStats().getGuestMemoryUsage();
					float currentMemoryUtilization = (float)currentMemoryUsage / (float)maxMemoryUsage;
					
					return new ResourceUtilization(currentCpuUtilization, currentMemoryUtilization);
				}
			}
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
		
		return null;
	}
}
