import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# Load the data
data = pd.read_csv('../results/tpcc_2025-04-24_20-01-34.samples.csv')

# Set the style for plots
plt.style.use('seaborn-v0_8-whitegrid')

# Create the throughput over time plot
plt.figure(figsize=(10, 6))
plt.plot(data['Time (seconds)'], data['Throughput (requests/second)'], color='#1f77b4', linewidth=2)
plt.title('Throughput over Time', fontsize=14)
plt.xlabel('Time (seconds)', fontsize=12)
plt.ylabel('Throughput (requests/second)', fontsize=12)
plt.grid(True, alpha=0.3)

# Add a trend line
z = np.polyfit(data['Time (seconds)'], data['Throughput (requests/second)'], 1)
p = np.poly1d(z)
plt.plot(data['Time (seconds)'], p(data['Time (seconds)']), "r--", alpha=0.7, 
         label=f'Trend: {z[0]:.2f}x + {z[1]:.2f}')

# Add mean line
plt.axhline(y=data['Throughput (requests/second)'].mean(), color='green', linestyle='-', alpha=0.5,
           label=f'Mean: {data["Throughput (requests/second)"].mean():.2f}')
plt.legend()

# Show the plot in a new window
plt.tight_layout()
plt.show()

# Print statistics
print(f"Average Throughput: {data['Throughput (requests/second)'].mean():.2f} requests/second")
print(f"Peak Throughput: {data['Throughput (requests/second)'].max():.2f} requests/second")
print(f"Minimum Throughput: {data['Throughput (requests/second)'].min():.2f} requests/second")