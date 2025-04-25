import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

# Load the data
data = pd.read_csv('../results/tpcc_2025-04-24_20-01-34.samples.csv')

# Set the style for plots
plt.style.use('seaborn-v0_8-whitegrid')
sns.set_palette("viridis")

# Create time bins
bins = 10
data['Time Bin'] = pd.cut(data['Time (seconds)'], bins=bins, labels=[f"{i}" for i in range(bins)])

# Prepare data for heatmap
heatmap_data = data.groupby('Time Bin')[['Median Latency (microseconds)', 
                                         '95th Percentile Latency (microseconds)', 
                                         '99th Percentile Latency (microseconds)']].mean() / 1000
heatmap_data.columns = ['Median', '95th', '99th']

# Create heatmap
plt.figure(figsize=(12, 6))
sns.heatmap(heatmap_data.T, annot=True, fmt='.1f', cmap='YlOrRd')
plt.title('Latency Percentiles Across Time Segments', fontsize=14)
plt.xlabel('Time Bin', fontsize=12)
plt.ylabel('Latency Percentile', fontsize=12)

# Show the plot in a new window
plt.tight_layout()
plt.show()

# Print time bin ranges
bin_ranges = pd.cut(data['Time (seconds)'], bins=bins).unique()
print("Time bin ranges (seconds):")
for i, bin_range in enumerate(sorted(bin_ranges)):
    print(f"Bin {i}: {bin_range}")