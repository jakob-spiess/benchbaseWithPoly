import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

# Load the data
data = pd.read_csv('../results/tpcc_2025-04-24_20-01-34.samples.csv')

# Set the style for plots
plt.style.use('seaborn-v0_8-whitegrid')
sns.set_palette("viridis")

# Create the latency percentiles over time plot
plt.figure(figsize=(10, 6))
latency_cols = ['Median Latency (microseconds)', 
                '95th Percentile Latency (microseconds)', 
                '99th Percentile Latency (microseconds)']

for col in latency_cols:
    plt.plot(data['Time (seconds)'], data[col]/1000, label=col.split()[0])
    
plt.title('Latency Percentiles over Time', fontsize=14)
plt.xlabel('Time (seconds)', fontsize=12)
plt.ylabel('Latency (milliseconds)', fontsize=12)
plt.legend()
plt.grid(True, alpha=0.3)

# Show the plot in a new window
plt.tight_layout()
plt.show()

# Print statistics
print(f"Average Median Latency: {data['Median Latency (microseconds)'].mean()/1000:.2f} ms")
print(f"Average 95th Percentile Latency: {data['95th Percentile Latency (microseconds)'].mean()/1000:.2f} ms")
print(f"Average 99th Percentile Latency: {data['99th Percentile Latency (microseconds)'].mean()/1000:.2f} ms")