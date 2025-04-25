import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np

# Load the data
data = pd.read_csv('./results/tpcc_2025-04-24_20-01-34.samples.csv')

# Set the style for plots
plt.style.use('seaborn-v0_8-whitegrid')
sns.set_palette("viridis")

# Create a figure with subplots for the main visualizations
fig = plt.figure(figsize=(18, 15))
fig.suptitle('TPC-C Benchmark Performance Analysis', fontsize=16)

# 1. Throughput over time
ax1 = plt.subplot(2, 2, 1)
ax1.plot(data['Time (seconds)'], data['Throughput (requests/second)'], color='#1f77b4', linewidth=2)
ax1.set_title('Throughput over Time')
ax1.set_xlabel('Time (seconds)')
ax1.set_ylabel('Throughput (requests/second)')
ax1.grid(True, alpha=0.3)

# Add a trend line
z = np.polyfit(data['Time (seconds)'], data['Throughput (requests/second)'], 1)
p = np.poly1d(z)
ax1.plot(data['Time (seconds)'], p(data['Time (seconds)']), "r--", alpha=0.7, 
         label=f'Trend: {z[0]:.2f}x + {z[1]:.2f}')
ax1.axhline(y=data['Throughput (requests/second)'].mean(), color='green', linestyle='-', alpha=0.5,
           label=f'Mean: {data["Throughput (requests/second)"].mean():.2f}')
ax1.legend()

# 2. Latency percentiles over time
ax2 = plt.subplot(2, 2, 2)
latency_cols = ['Median Latency (microseconds)', 
                '95th Percentile Latency (microseconds)', 
                '99th Percentile Latency (microseconds)']
for col in latency_cols:
    ax2.plot(data['Time (seconds)'], data[col]/1000, label=col.split()[0])
ax2.set_title('Latency Percentiles over Time')
ax2.set_xlabel('Time (seconds)')
ax2.set_ylabel('Latency (milliseconds)')
ax2.legend()
ax2.grid(True, alpha=0.3)

# 3. Throughput vs Latency scatter plot
ax3 = plt.subplot(2, 2, 3)
sc = ax3.scatter(data['Throughput (requests/second)'], 
                data['Median Latency (microseconds)']/1000, 
                c=data['Time (seconds)'], cmap='viridis', alpha=0.7)
plt.colorbar(sc, label='Time (seconds)')
ax3.set_title('Throughput vs Median Latency')
ax3.set_xlabel('Throughput (requests/second)')
ax3.set_ylabel('Median Latency (milliseconds)')
ax3.grid(True, alpha=0.3)

# 4. Latency distribution box plot
ax4 = plt.subplot(2, 2, 4)
latency_data = data[['Minimum Latency (microseconds)', 
                     '25th Percentile Latency (microseconds)',
                     'Median Latency (microseconds)', 
                     '75th Percentile Latency (microseconds)',
                     '95th Percentile Latency (microseconds)',
                     '99th Percentile Latency (microseconds)',
                     'Maximum Latency (microseconds)']]
latency_data = latency_data / 1000  # Convert to milliseconds
latency_data.columns = ['Min', '25th', 'Median', '75th', '95th', '99th', 'Max']
sns.boxplot(data=latency_data, ax=ax4)
ax4.set_title('Latency Distribution (Entire Test)')
ax4.set_ylabel('Latency (milliseconds)')
ax4.grid(True, alpha=0.3)
ax4.set_yscale('log')  # Log scale for better visualization
ax4.set_ylim(bottom=1)

plt.tight_layout(rect=[0, 0.03, 1, 0.95])

# Additional plot: Heatmap of time bins vs latency percentiles
plt.figure(figsize=(12, 8))
# Create time bins
bins = 10
data['Time Bin'] = pd.cut(data['Time (seconds)'], bins=bins, labels=[f"{i}" for i in range(bins)])

# Prepare data for heatmap
heatmap_data = data.groupby('Time Bin')[['Median Latency (microseconds)', 
                                         '95th Percentile Latency (microseconds)', 
                                         '99th Percentile Latency (microseconds)']].mean() / 1000
heatmap_data.columns = ['Median', '95th', '99th']

# Plot heatmap
sns.heatmap(heatmap_data.T, annot=True, fmt='.1f', cmap='YlOrRd')
plt.title('Latency Percentiles Across Time Segments')
plt.xlabel('Time Bin')
plt.ylabel('Latency Percentile')

# Additional plot: Moving average of throughput
plt.figure(figsize=(12, 6))
window_size = 10
data['Throughput MA'] = data['Throughput (requests/second)'].rolling(window=window_size).mean()

plt.plot(data['Time (seconds)'], data['Throughput (requests/second)'], 
         alpha=0.4, label='Raw Throughput')
plt.plot(data['Time (seconds)'], data['Throughput MA'], 
         linewidth=2, label=f'Moving Average (window={window_size})')
plt.title(f'Throughput with {window_size}-second Moving Average')
plt.xlabel('Time (seconds)')
plt.ylabel('Throughput (requests/second)')
plt.legend()
plt.grid(True, alpha=0.3)

# Display all plots
plt.tight_layout()
plt.show()

# Save the figures for the presentation
fig.savefig('tpcc_performance_overview.png', dpi=300, bbox_inches='tight')

# Bonus: Calculate and print key statistics for the slides
print("\nKey Statistics for the Presentation:")
print(f"Average Throughput: {data['Throughput (requests/second)'].mean():.2f} requests/second")
print(f"Peak Throughput: {data['Throughput (requests/second)'].max():.2f} requests/second")
print(f"Average Median Latency: {data['Median Latency (microseconds)'].mean()/1000:.2f} ms")
print(f"Average 95th Percentile Latency: {data['95th Percentile Latency (microseconds)'].mean()/1000:.2f} ms")
print(f"Average 99th Percentile Latency: {data['99th Percentile Latency (microseconds)'].mean()/1000:.2f} ms")