package mapreduce;

public class ResourceUtilization
{
	private float _cpuUtilization;
	private float _memoryUtilization;
	
	public ResourceUtilization(float cpuUtilization, float memoryUtilization)
	{
		_cpuUtilization = cpuUtilization;
		_memoryUtilization = memoryUtilization;
	}
	
	// The percentage (0 - 1) of CPU currently being used.
	public float getCpuUtilization()
	{
		return _cpuUtilization;
	}
	
	// The percentage (0 - 1) of RAM currently being used.
	public float getMemoryUtilization()
	{
		return _memoryUtilization;
	}
}
