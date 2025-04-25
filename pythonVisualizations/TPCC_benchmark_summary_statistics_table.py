import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# Load the data
data = pd.read_csv('../results/tpcc_2025-04-24_20-01-34.samples.csv')

# Calculate overall summary statistics
summary = {
    'Metric': [
        'Average Throughput (req/sec)', 
        'Peak Throughput (req/sec)',
        'Throughput Stability (CoV)',
        'Average Requests per Second',
        'Total Requests',
        'Average Median Latency (ms)',
        'Average 95th Percentile Latency (ms)',
        'Average 99th Percentile Latency (ms)',
        'Maximum Latency (ms)',
        'Test Duration (seconds)'
    ],
    'Value': [
        f"{data['Throughput (requests/second)'].mean():.2f}",
        f"{data['Throughput (requests/second)'].max():.2f}",
        f"{data['Throughput (requests/second)'].std() / data['Throughput (requests/second)'].mean():.4f}",
        f"{data['Requests'].mean():.2f}",
        f"{data['Requests'].sum()}",
        f"{data['Median Latency (microseconds)'].mean()/1000:.2f}",
        f"{data['95th Percentile Latency (microseconds)'].mean()/1000:.2f}",
        f"{data['99th Percentile Latency (microseconds)'].mean()/1000:.2f}",
        f"{data['Maximum Latency (microseconds)'].max()/1000:.2f}",
        f"{data['Time (seconds)'].max()}"
    ]
}

# Create summary table visualization
fig, ax = plt.figure(figsize=(10, 8)), plt.subplot(111)
ax.axis('tight')
ax.axis('off')
table = ax.table(cellText=list(zip(summary['Metric'], summary['Value'])), 
                 colLabels=['Metric', 'Value'],
                 loc='center', cellLoc='left')
table.auto_set_font_size(False)
table.set_fontsize(12)
table.scale(1.2, 1.5)
table.auto_set_column_width([0, 1])

plt.title('TPC-C Benchmark Summary Statistics', fontsize=16)

# Show the plot in a new window
plt.tight_layout()
plt.show()

# Print all statistics in text format
print("TPC-C Benchmark Summary Statistics:")
for metric, value in zip(summary['Metric'], summary['Value']):
    print(f"{metric}: {value}")